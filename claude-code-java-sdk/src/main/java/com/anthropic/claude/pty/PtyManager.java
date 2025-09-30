package com.anthropic.claude.pty;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Pty4J伪终端管理器
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class PtyManager {
    private static final Logger logger = LoggerFactory.getLogger(PtyManager.class);

    private PtyProcess ptyProcess;
    private OutputParser outputParser;
    private BufferedWriter inputWriter;
    private ExecutorService executorService;
    private ClaudeState currentState = ClaudeState.STARTING;

    // 回调接口
    private Consumer<ClaudeResponse> outputListener;
    private Consumer<StateChange> stateChangeListener;
    private Consumer<String> errorListener;

    // 配置
    private int terminalWidth = 120;
    private int terminalHeight = 30;
    private Duration readyTimeout = Duration.ofSeconds(45); // 增加默认超时时间

    // 状态管理
    private volatile boolean isRunning = false;
    private Instant lastInteraction;

    public PtyManager() {
        this.outputParser = new OutputParser();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "PtyManager-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 启动伪终端
     *
     * @param command 启动命令
     * @throws IOException 如果启动失败
     */
    public void startPty(String[] command) throws IOException {
        if (isRunning) {
            throw new IllegalStateException("Pty进程已经在运行中");
        }

        logger.info("启动Pty进程: {}", String.join(" ", command));

        try {
            // 设置环境变量
            Map<String, String> envs = new HashMap<>(System.getenv());
            envs.put("TERM", "xterm-256color");
            envs.put("LANG", "en_US.UTF-8");
            envs.put("LC_ALL", "en_US.UTF-8");

            // 创建伪终端进程
            ptyProcess = new PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(envs)
                .setDirectory(System.getProperty("user.dir"))
                .setInitialColumns(terminalWidth)
                .setInitialRows(terminalHeight)
                .setConsole(false)
                .start();

            // 创建输入写入器
            inputWriter = new BufferedWriter(
                new OutputStreamWriter(ptyProcess.getOutputStream(), StandardCharsets.UTF_8));

            isRunning = true;
            lastInteraction = Instant.now();

            // 启动输出读取线程
            startOutputReader();

            // 等待CLI就绪（可能超时抛出异常）
            waitForReadyOrThrow();

            logger.info("Pty进程启动成功");

        } catch (IOException e) {
            isRunning = false;
            logger.error("启动Pty进程失败", e);
            throw e;
        }
    }

    /**
     * 发送命令
     *
     * @param command 要发送的命令
     * @return 响应的CompletableFuture
     */
    public CompletableFuture<ClaudeResponse> sendCommand(String command) {
        if (!isRunning || ptyProcess == null || !ptyProcess.isAlive()) {
            CompletableFuture<ClaudeResponse> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("Pty进程未运行"));
            return failedFuture;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("发送命令: {}", command);

                synchronized (inputWriter) {
                    inputWriter.write(command);
                    inputWriter.newLine();
                    inputWriter.flush();
                }

                lastInteraction = Instant.now();

                // 创建一个简单的响应（实际响应通过回调处理）
                return new ClaudeResponse(ClaudeResponse.ClaudeResponseType.STATUS_UPDATE,
                    "命令已发送: " + command);

            } catch (IOException e) {
                logger.error("发送命令失败: {}", command, e);
                throw new RuntimeException("发送命令失败", e);
            }
        }, executorService);
    }

    /**
     * 发送中断信号 (Ctrl+C)
     */
    public void sendInterrupt() {
        if (isRunning && ptyProcess != null && ptyProcess.isAlive()) {
            try {
                // 发送 Ctrl+C (ASCII 3)
                synchronized (inputWriter) {
                    inputWriter.write('\u0003');
                    inputWriter.flush();
                }
                logger.debug("发送中断信号");
            } catch (IOException e) {
                logger.error("发送中断信号失败", e);
            }
        }
    }

    /**
     * 关闭伪终端
     */
    public void closePty() {
        if (!isRunning) {
            return;
        }

        logger.info("关闭Pty进程");
        isRunning = false;

        try {
            // 关闭输入流
            if (inputWriter != null) {
                inputWriter.close();
            }

            // 终止进程
            if (ptyProcess != null && ptyProcess.isAlive()) {
                ptyProcess.destroy();

                // 等待进程结束
                if (!ptyProcess.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("强制终止Pty进程");
                    ptyProcess.destroyForcibly();
                }
            }

        } catch (Exception e) {
            logger.error("关闭Pty进程时出错", e);
        } finally {
            // 关闭线程池
            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 检查进程状态
     */
    public boolean isAlive() {
        return isRunning && ptyProcess != null && ptyProcess.isAlive();
    }

    /**
     * 设置窗口大小
     */
    public void setWindowSize(int width, int height) {
        this.terminalWidth = width;
        this.terminalHeight = height;

        if (ptyProcess != null && ptyProcess.isAlive()) {
            ptyProcess.setWinSize(new WinSize(width, height));
        }
    }

    /**
     * 启动输出读取线程
     */
    private void startOutputReader() {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ptyProcess.getInputStream(), StandardCharsets.UTF_8))) {

                char[] buffer = new char[4096];
                StringBuilder lineBuffer = new StringBuilder();

                while (isRunning && ptyProcess.isAlive()) {
                    int bytesRead = reader.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        String chunk = new String(buffer, 0, bytesRead);

                        // 按行处理输出（兼容 CRLF/CR）
                        lineBuffer.append(chunk);
                        String[] lines = lineBuffer.toString().split("(\\r\\n|\\n|\\r)", -1);

                        // 处理完整的行
                        for (int i = 0; i < lines.length - 1; i++) {
                            processOutputLine(lines[i]);
                        }

                        // 保留最后不完整的行
                        lineBuffer.setLength(0);
                        lineBuffer.append(lines[lines.length - 1]);
                    }
                }

                // 处理剩余内容
                if (lineBuffer.length() > 0) {
                    processOutputLine(lineBuffer.toString());
                }

            } catch (IOException e) {
                if (isRunning) {
                    logger.error("读取Pty输出时出错", e);
                    if (errorListener != null) {
                        errorListener.accept("读取输出失败: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 处理输出行
     */
    private void processOutputLine(String line) {
        try {
            List<StateChange> changes = outputParser.parseOutput(line);

            for (StateChange change : changes) {
                // 更新当前状态
                if (change.getToState() != null) {
                    ClaudeState previousState = currentState;
                    currentState = change.getToState();

                    // 通知状态变化
                    if (stateChangeListener != null) {
                        stateChangeListener.accept(change);
                    }

                    logger.debug("状态变化: {} -> {}", previousState, currentState);
                }

                // 通知响应
                if (change.getResponse() != null && outputListener != null) {
                    outputListener.accept(change.getResponse());
                }
            }

        } catch (Exception e) {
            logger.warn("处理输出行时出错: {}", line, e);
        }
    }

    /**
     * 等待CLI就绪
     */
    private void waitForReady() throws IOException {
        Instant deadline = Instant.now().plus(readyTimeout);

        while (Instant.now().isBefore(deadline) && isRunning) {
            if (currentState == ClaudeState.READY || currentState == ClaudeState.WAITING_INPUT) {
                logger.debug("CLI已就绪");
                return;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("等待就绪时被中断", e);
            }
        }

        if (isRunning) {
            logger.warn("等待CLI就绪超时，当前状态: {}", currentState);
        }
    }
    
    /**
     * 等待CLI就绪（超时抛出异常，供上层回退使用）
     */
    private void waitForReadyOrThrow() throws IOException {
        Instant deadline = Instant.now().plus(readyTimeout);
        logger.debug("开始等待CLI就绪，超时时间: {}秒", readyTimeout.getSeconds());

        while (Instant.now().isBefore(deadline) && isRunning) {
            logger.debug("当前状态: {}, 进程状态: alive={}", currentState, ptyProcess != null && ptyProcess.isAlive());

            // 扩展就绪状态判断条件
            if (currentState == ClaudeState.READY ||
                currentState == ClaudeState.WAITING_INPUT ||
                currentState == ClaudeState.PROCESSING ||
                (ptyProcess != null && ptyProcess.isAlive() && currentState != ClaudeState.STARTING)) {
                logger.info("CLI已就绪，最终状态: {}", currentState);
                return;
            }

            try {
                Thread.sleep(200); // 增加等待间隔，减少CPU占用
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("等待就绪时被中断", e);
            }
        }

        // 添加更详细的错误信息
        String processStatus = ptyProcess != null ?
            (ptyProcess.isAlive() ? "进程运行中" : "进程已停止") : "进程未启动";
        throw new IOException(String.format("等待CLI就绪超时，当前状态: %s, %s", currentState, processStatus));
    }

    // Setters for callbacks
    public void setOutputListener(Consumer<ClaudeResponse> outputListener) {
        this.outputListener = outputListener;
    }

    public void setStateChangeListener(Consumer<StateChange> stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }

    public void setErrorListener(Consumer<String> errorListener) {
        this.errorListener = errorListener;
    }

    // Getters
    public ClaudeState getCurrentState() {
        return currentState;
    }

    public Instant getLastInteraction() {
        return lastInteraction;
    }

    public void setReadyTimeout(Duration readyTimeout) {
        this.readyTimeout = readyTimeout;
    }
}
