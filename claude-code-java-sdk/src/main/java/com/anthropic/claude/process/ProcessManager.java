package com.anthropic.claude.process;

import com.anthropic.claude.exceptions.ProcessExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class ProcessManager {
    private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);

    private final Duration defaultTimeout;
    private final Map<String, String> environment;

    public ProcessManager() {
        this(Duration.ofMinutes(10), new HashMap<>());
    }

    public ProcessManager(Duration defaultTimeout, Map<String, String> environment) {
        this.defaultTimeout = defaultTimeout;
        this.environment = environment;
    }

    public ProcessResult executeSync(List<String> command) throws ProcessExecutionException {
        return executeSync(command, defaultTimeout);
    }

    public ProcessResult executeSync(List<String> command, Duration timeout) throws ProcessExecutionException {
        try {
            logger.debug("执行命令: {}", String.join(" ", command));

            ProcessResult result = new ProcessExecutor()
                    .command(command)
                    .environment(environment)
                    .timeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .readOutput(true)
                    .execute();

            if (result.getExitValue() != 0) {
                String errorMsg = String.format("命令执行失败，退出码: %d, 错误信息: %s",
                    result.getExitValue(), result.outputUTF8());
                throw new ProcessExecutionException(errorMsg, result.getExitValue());
            }

            logger.debug("命令执行成功，输出: {}", result.outputUTF8());
            return result;
        } catch (IOException | TimeoutException | InterruptedException e) {
            String errorMsg = String.format("命令执行异常: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new ProcessExecutionException(errorMsg, -1, e);
        }
    }

    public CompletableFuture<ProcessResult> executeAsync(List<String> command) {
        return executeAsync(command, defaultTimeout);
    }

    public CompletableFuture<ProcessResult> executeAsync(List<String> command, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeSync(command, timeout);
            } catch (ProcessExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void executeStreaming(List<String> command, Consumer<String> outputConsumer)
            throws ProcessExecutionException {
        executeStreaming(command, outputConsumer, defaultTimeout);
    }

    public void executeStreaming(List<String> command, Consumer<String> outputConsumer, Duration timeout)
            throws ProcessExecutionException {
        try {
            logger.debug("开始流式执行命令: {}", String.join(" ", command));

            ProcessResult result = new ProcessExecutor()
                    .command(command)
                    .environment(environment)
                    .timeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            outputConsumer.accept(line);
                        }
                    })
                    .execute();

            if (result.getExitValue() != 0) {
                String errorMsg = String.format("流式命令执行失败，退出码: %d", result.getExitValue());
                throw new ProcessExecutionException(errorMsg, result.getExitValue());
            }

            logger.debug("流式命令执行完成");
        } catch (IOException | TimeoutException | InterruptedException e) {
            String errorMsg = String.format("流式命令执行异常: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new ProcessExecutionException(errorMsg, -1, e);
        }
    }

    public CompletableFuture<Void> executeStreamingAsync(List<String> command,
                                                        Consumer<String> outputConsumer) {
        return executeStreamingAsync(command, outputConsumer, defaultTimeout);
    }

    public CompletableFuture<Void> executeStreamingAsync(List<String> command,
                                                        Consumer<String> outputConsumer,
                                                        Duration timeout) {
        return CompletableFuture.runAsync(() -> {
            try {
                executeStreaming(command, outputConsumer, timeout);
            } catch (ProcessExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isCommandAvailable(String command) {
        try {
            ProcessResult result = new ProcessExecutor()
                    .command(getWhichCommand(), command)
                    .readOutput(true)
                    .execute();
            return result.getExitValue() == 0;
        } catch (Exception e) {
            logger.debug("检查命令可用性时出错: {}", e.getMessage());
            return false;
        }
    }

    private String getWhichCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "where" : "which";
    }
}