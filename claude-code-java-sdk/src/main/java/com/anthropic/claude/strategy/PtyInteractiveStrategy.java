package com.anthropic.claude.strategy;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageParser;
import com.anthropic.claude.pty.ClaudeResponse;
import com.anthropic.claude.pty.PtyManager;
import com.anthropic.claude.pty.StateChange;
import com.anthropic.claude.query.QueryRequest;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * PTY交互执行策略
 *
 * 维护常驻会话，通过回调接收响应
 * 异常时自动回退至批处理模式
 *
 * @author Claude Code SDK
 */
public class PtyInteractiveStrategy implements CliExecutionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(PtyInteractiveStrategy.class);

    private final PtyManager ptyManager;
    private final MessageParser messageParser;
    private final ClaudeCodeOptions options;
    private final BatchProcessStrategy fallbackStrategy;

    private final AtomicBoolean isReady = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final List<ClaudeResponse> responseBuffer = new ArrayList<>();

    public PtyInteractiveStrategy(PtyManager ptyManager,
                                 MessageParser messageParser,
                                 ClaudeCodeOptions options,
                                 BatchProcessStrategy fallbackStrategy) {
        this.ptyManager = ptyManager;
        this.messageParser = messageParser;
        this.options = options;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public void start() throws ClaudeCodeException {
        logger.info("启动PTY交互策略");

        try {
            // 设置回调监听器
            setupCallbacks();

            // 启动PTY会话
            List<String> command = buildPtyCommand();
            logger.debug("PTY命令: {}", String.join(" ", command));

            ptyManager.startPty(command.toArray(new String[0]));

            // 等待进程就绪
            waitForReady();

            isReady.set(true);
            logger.info("PTY交互策略启动成功");

        } catch (Exception e) {
            logger.error("PTY策略启动失败，将回退到批处理模式", e);
            throw new ClaudeCodeException("PTY策略启动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() throws ClaudeCodeException {
        logger.info("关闭PTY交互策略");

        isShutdown.set(true);
        isReady.set(false);

        try {
            ptyManager.closePty();
            logger.info("PTY交互策略关闭完成");

        } catch (Exception e) {
            logger.error("PTY策略关闭时发生错误", e);
            throw new ClaudeCodeException("PTY策略关闭失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Stream<Message> execute(QueryRequest request) throws ClaudeCodeException {
        if (!isAvailable()) {
            logger.warn("PTY策略不可用，回退到批处理模式");
            return fallbackStrategy.execute(request);
        }

        logger.debug("执行PTY交互查询: {}", request.getPrompt());

        try {
            // 发送查询命令
            String queryLine = buildQueryLine(request);
            CompletableFuture<ClaudeResponse> future = ptyManager.sendCommand(queryLine);

            // 等待响应完成
            Duration timeout = request.getTimeout() != null ? request.getTimeout() : Duration.ofMinutes(10);
            future.get(timeout.getSeconds(), TimeUnit.SECONDS);

            // 从响应缓冲区获取完整响应
            String response = collectResponseFromBuffer();
            List<Message> messages = messageParser.parseMessages(response);
            return messages.stream();

        } catch (Exception e) {
            logger.error("PTY执行失败，回退到批处理模式", e);
            return fallbackStrategy.execute(request);
        }
    }

    @Override
    public Observable<Message> executeStream(QueryRequest request) throws ClaudeCodeException {
        if (!isAvailable()) {
            logger.warn("PTY策略不可用，回退到批处理模式");
            return fallbackStrategy.executeStream(request);
        }

        logger.debug("执行PTY交互流式查询: {}", request.getPrompt());

        return Observable.create(emitter -> {
            try {
                // 设置输出监听器用于流式处理
                ptyManager.setOutputListener(response -> {
                    try {
                        Stream<Message> messages = messageParser.parseStreamingMessages(
                            response.getContent());
                        messages.forEach(emitter::onNext);
                    } catch (Exception e) {
                        logger.debug("解析流式消息失败: {}", e.getMessage());
                    }
                });

                // 发送查询命令
                String queryLine = buildQueryLine(request);
                ptyManager.sendCommand(queryLine);

            } catch (Exception e) {
                logger.error("PTY流式执行失败，回退到批处理模式", e);

                // 回退到批处理流式执行
                fallbackStrategy.executeStream(request).subscribe(
                    emitter::onNext,
                    emitter::onError,
                    emitter::onComplete
                );
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return !isShutdown.get() && isReady.get() && ptyManager.isAlive();
    }

    @Override
    public String getStrategyType() {
        return "PtyInteractive";
    }

    @Override
    public boolean supportsSessionPersistence() {
        // PTY模式天然支持会话保持
        return true;
    }

    /**
     * 设置PTY回调监听器
     */
    private void setupCallbacks() {
        ptyManager.setOutputListener(response -> {
            synchronized (responseBuffer) {
                responseBuffer.add(response);
            }
        });

        ptyManager.setStateChangeListener(stateChange -> {
            logger.debug("PTY状态变化: {}", stateChange);
        });

        ptyManager.setErrorListener(error -> {
            logger.error("PTY错误: {}", error);
        });
    }

    /**
     * 等待PTY进程就绪
     */
    private void waitForReady() throws TimeoutException {
        Duration timeout = options.getPtyReadyTimeout() != null ?
            options.getPtyReadyTimeout() : Duration.ofSeconds(15); // 增加默认超时

        logger.info("等待PTY进程就绪，超时时间: {}秒", timeout.getSeconds());

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < timeout.toMillis()) {
            // 更宽松的就绪检测条件
            if (ptyManager.isAlive()) {
                // 额外等待1秒确保状态稳定
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TimeoutException("等待就绪时被中断");
                }

                logger.info("PTY进程已就绪，当前状态: {}", ptyManager.getCurrentState());
                return;
            }

            try {
                Thread.sleep(200); // 增加检查间隔
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TimeoutException("等待就绪时被中断");
            }
        }

        logger.warn("PTY进程启动超时，但会继续尝试使用，当前状态: {}", ptyManager.getCurrentState());
        // 不抛出异常，而是记录警告并继续
    }

    /**
     * 构建PTY命令
     */
    private List<String> buildPtyCommand() {
        List<String> command = new ArrayList<>();

        command.add(options.getCliPath());

        // PTY模式不强制--print，可通过配置注入additionalArgs
        if (options.getAdditionalArgs() != null) {
            command.addAll(options.getAdditionalArgs());
        }

        return command;
    }

    /**
     * 构建查询行
     */
    private String buildQueryLine(QueryRequest request) {
        StringBuilder query = new StringBuilder();

        query.append(request.getPrompt());

        // 添加工具参数
        if (request.getTools() != null && request.getTools().length > 0) {
            for (String tool : request.getTools()) {
                query.append(" --tool ").append(tool);
            }
        }

        return query.toString();
    }

    /**
     * 从响应缓冲区收集完整响应
     */
    private String collectResponseFromBuffer() {
        StringBuilder response = new StringBuilder();
        synchronized (responseBuffer) {
            for (ClaudeResponse claudeResponse : responseBuffer) {
                response.append(claudeResponse.getContent()).append("\n");
            }
            responseBuffer.clear();
        }
        return response.toString();
    }
}