package com.anthropic.claude.transport;

import com.anthropic.claude.config.ClaudeAgentOptions;
import com.anthropic.claude.process.ProcessManager;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 CLI 进程的传输层实现
 *
 * <p>通过启动和管理 Claude CLI 进程来实现与 Claude 的通信。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * ProcessManager processManager = new ProcessManager();
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .cliPath("/usr/local/bin/claude-code")
 *     .build();
 *
 * try (ProcessTransport transport = new ProcessTransport(processManager, options)) {
 *     transport.send("Hello, Claude!");
 *     transport.receive()
 *         .subscribe(line -> System.out.println("Response: " + line));
 * }
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class ProcessTransport implements ClaudeTransport {
    private static final Logger logger = LoggerFactory.getLogger(ProcessTransport.class);

    private final ProcessManager processManager;
    private final ClaudeAgentOptions options;
    private Process cliProcess;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final TransportStats stats = new TransportStats();

    /**
     * 创建 ProcessTransport 实例
     *
     * @param processManager 进程管理器
     * @param options 配置选项
     */
    public ProcessTransport(ProcessManager processManager, ClaudeAgentOptions options) {
        this.processManager = Objects.requireNonNull(processManager, "processManager cannot be null");
        this.options = Objects.requireNonNull(options, "options cannot be null");
    }

    @Override
    public void send(String message) throws TransportException {
        Objects.requireNonNull(message, "message cannot be null");

        try {
            ensureConnected();

            OutputStream stdin = cliProcess.getOutputStream();
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            stdin.write(bytes);
            stdin.flush();

            stats.incrementMessagesSent();
            stats.addBytesSent(bytes.length);

            logger.debug("Sent message: {} bytes", bytes.length);

        } catch (IOException e) {
            stats.incrementErrors();
            throw new TransportException("Failed to send message", e);
        }
    }

    @Override
    public Observable<String> receive() throws TransportException {
        ensureConnected();

        return Observable.create(emitter -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(cliProcess.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    stats.incrementMessagesReceived();
                    stats.addBytesReceived(line.length());

                    logger.debug("Received line: {} chars", line.length());
                    emitter.onNext(line);
                }

                emitter.onComplete();
                logger.debug("Stream completed");

            } catch (IOException e) {
                stats.incrementErrors();
                logger.error("Error reading from process", e);
                emitter.onError(new TransportException("Failed to receive message", e));
            }
        });
    }

    @Override
    public boolean isConnected() {
        return connected.get() && cliProcess != null && cliProcess.isAlive();
    }

    @Override
    public void close() throws TransportException {
        if (cliProcess != null) {
            logger.info("Closing CLI process");

            cliProcess.destroy();

            try {
                if (!cliProcess.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("Process did not terminate gracefully, forcing...");
                    cliProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransportException("Interrupted while closing process", e);
            }

            connected.set(false);
            logger.info("ProcessTransport closed. Stats: {}", stats);
        }
    }

    @Override
    public TransportType getType() {
        return TransportType.CLI_PROCESS;
    }

    @Override
    public TransportStats getStats() {
        return stats;
    }

    @Override
    public void reset() throws TransportException {
        logger.info("Resetting ProcessTransport");

        // 关闭现有进程
        if (cliProcess != null && cliProcess.isAlive()) {
            close();
        }

        // 重置统计
        stats.reset();
        connected.set(false);

        logger.info("ProcessTransport reset complete");
    }

    /**
     * 确保 CLI 进程已启动并连接
     *
     * @throws TransportException 如果无法启动进程
     */
    private void ensureConnected() throws TransportException {
        if (!isConnected()) {
            startCliProcess();
        }
    }

    /**
     * 启动 CLI 进程
     *
     * @throws TransportException 如果启动失败
     */
    private void startCliProcess() throws TransportException {
        try {
            List<String> command = buildCommand();

            logger.info("Starting CLI process: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 设置环境变量
            if (!options.getEnvironment().isEmpty()) {
                processBuilder.environment().putAll(options.getEnvironment());
            }

            // 启动进程
            this.cliProcess = processBuilder.start();
            connected.set(true);

            logger.info("CLI process started successfully (PID: {})",
                cliProcess.pid());

        } catch (IOException e) {
            connected.set(false);
            stats.incrementErrors();
            throw new TransportException("Failed to start CLI process", e);
        }
    }

    /**
     * 构建 CLI 命令
     *
     * @return 命令列表
     */
    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();

        // CLI 可执行文件路径
        command.add(options.getCliPath());

        // 添加额外参数
        command.addAll(options.getAdditionalArgs());

        return command;
    }

    @Override
    public String toString() {
        return String.format(
            "ProcessTransport{connected=%s, cliPath='%s', stats=%s}",
            isConnected(), options.getCliPath(), stats
        );
    }
}
