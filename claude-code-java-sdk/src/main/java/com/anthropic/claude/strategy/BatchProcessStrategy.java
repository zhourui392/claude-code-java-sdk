package com.anthropic.claude.strategy;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageParser;
import com.anthropic.claude.process.ProcessManager;
import com.anthropic.claude.query.QueryRequest;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 批处理执行策略
 *
 * 每次调用启动CLI，读取输出后进程结束
 * 默认采用 --print 参数，流式采用 --output-format json-stream --stream
 *
 * @author Claude Code SDK
 */
public class BatchProcessStrategy implements CliExecutionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessStrategy.class);

    private final ProcessManager processManager;
    private final MessageParser messageParser;
    private final ClaudeCodeOptions options;

    public BatchProcessStrategy(ProcessManager processManager,
                               MessageParser messageParser,
                               ClaudeCodeOptions options) {
        this.processManager = processManager;
        this.messageParser = messageParser;
        this.options = options;
    }

    @Override
    public void start() throws ClaudeCodeException {
        logger.info("批处理策略启动 - 每次查询独立启动CLI进程");
        // 批处理模式无需初始化，每次查询时启动进程
    }

    @Override
    public void shutdown() throws ClaudeCodeException {
        logger.info("批处理策略关闭 - 无需特殊清理");
        // 批处理模式无需特殊关闭操作
    }

    @Override
    public Stream<Message> execute(QueryRequest request) throws ClaudeCodeException {
        logger.debug("执行批处理查询: {}", request.getPrompt());

        try {
            List<String> command = buildBatchCommand(request, false);
            logger.debug("批处理命令: {}", String.join(" ", command));

            ProcessResult result = processManager.executeSync(command,
                request.getTimeout() != null ? request.getTimeout() : Duration.ofMinutes(10));

            if (result.getExitValue() != 0) {
                String errorOutput = result.outputUTF8();
                logger.error("CLI执行失败，退出码: {}, 输出: {}", result.getExitValue(), errorOutput);
                throw new ClaudeCodeException("CLI执行失败: " + errorOutput);
            }

            String output = result.outputUTF8();
            logger.debug("CLI输出长度: {} 字符", output.length());

            List<Message> messages = messageParser.parseMessages(output);
            return messages.stream();

        } catch (Exception e) {
            logger.error("批处理执行失败", e);
            throw new ClaudeCodeException("批处理执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Observable<Message> executeStream(QueryRequest request) throws ClaudeCodeException {
        logger.debug("执行批处理流式查询: {}", request.getPrompt());

        return Observable.create(emitter -> {
            try {
                List<String> command = buildBatchCommand(request, true);
                logger.debug("批处理流式命令: {}", String.join(" ", command));

                CompletableFuture<ProcessResult> future = processManager.executeAsync(command);

                // 模拟流式处理 - 实际上批处理模式仍然是一次性返回
                ProcessResult result = future.get();

                if (result.getExitValue() != 0) {
                    String errorOutput = result.outputUTF8();
                    logger.error("CLI流式执行失败，退出码: {}, 输出: {}", result.getExitValue(), errorOutput);
                    emitter.onError(new ClaudeCodeException("CLI流式执行失败: " + errorOutput));
                    return;
                }

                String output = result.outputUTF8();
                Stream<Message> messages = messageParser.parseStreamingMessages(output);

                messages.forEach(emitter::onNext);
                emitter.onComplete();

            } catch (Exception e) {
                logger.error("批处理流式执行失败", e);
                emitter.onError(new ClaudeCodeException("批处理流式执行失败: " + e.getMessage(), e));
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return processManager != null && processManager.isCommandAvailable(options.getCliPath());
    }

    @Override
    public String getStrategyType() {
        return "BatchProcess";
    }

    @Override
    public boolean supportsSessionPersistence() {
        // 批处理模式通过 --continue/--resume 支持会话保持
        return true;
    }

    /**
     * 构建批处理命令
     *
     * @param request 查询请求
     * @param streaming 是否流式输出
     * @return 命令列表
     */
    private List<String> buildBatchCommand(QueryRequest request, boolean streaming) {
        List<String> command = new ArrayList<>();

        // 基础CLI路径
        command.add(options.getCliPath());

        // 添加提示词
        command.add(request.getPrompt());

        if (streaming) {
            // 流式输出参数
            command.add("--output-format");
            command.add("json-stream");
            command.add("--stream");
        } else {
            // 标准输出参数（默认--print）
            command.add("--print");
        }

        // 添加工具参数
        if (request.getTools() != null && request.getTools().length > 0) {
            for (String tool : request.getTools()) {
                command.add("--tool");
                command.add(tool);
            }
        }

        // 添加最大token数
        if (request.getMaxTokens() != null && request.getMaxTokens() > 0) {
            command.add("--max-tokens");
            command.add(String.valueOf(request.getMaxTokens()));
        }

        // 添加温度参数
        if (request.getTemperature() != null && request.getTemperature() > 0) {
            command.add("--temperature");
            command.add(String.valueOf(request.getTemperature()));
        }

        // 添加会话相关参数（如果QueryRequest支持的话）
        // TODO: 根据实际QueryRequest API添加会话支持
        /*
        if (request.getSessionId() != null) {
            command.add("--resume");
            command.add(request.getSessionId());
        } else if (request.isContinueSession()) {
            command.add("--continue");
        }
        */

        // 添加额外参数
        if (options.getAdditionalArgs() != null) {
            command.addAll(options.getAdditionalArgs());
        }

        return command;
    }
}