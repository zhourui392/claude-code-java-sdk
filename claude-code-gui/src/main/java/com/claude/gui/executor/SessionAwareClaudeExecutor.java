package com.claude.gui.executor;

import com.anthropic.claude.config.CliMode;
import com.claude.gui.callback.MessageCallback;
import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.auth.DefaultAuthenticationProvider;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.pty.PtyManager;
import com.anthropic.claude.pty.ClaudeResponse;
import com.anthropic.claude.pty.StateChange;
import com.anthropic.claude.pty.ClaudeState;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * 支持会话的Claude CLI执行器类，使用Pty4J进行伪终端交互
 * 提供更好的CLI交互体验和会话连续性
 *
 * @author Claude Code GUI Team
 * @version 3.0.0
 */
public class SessionAwareClaudeExecutor {

    private ClaudeCodeSDK claudeSDK;
    private PtyManager ptyManager;
    private MessageCallback callback;
    private volatile boolean isRunning;
    private ClaudeCliConfig config;
    private ExecutorService executorService;
    private String currentSessionId;
    private boolean useResume;
    private boolean usePtyMode;

    /**
     * 构造函数
     */
    public SessionAwareClaudeExecutor() {
        this.isRunning = false;
        this.config = new ClaudeCliConfig();
        this.useResume = true; // 默认启用resume功能
        this.usePtyMode = true; // 默认启用Pty模式
        this.currentSessionId = null;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SessionAwareClaudeExecutor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 构造函数
     *
     * @param config Claude CLI配置
     */
    public SessionAwareClaudeExecutor(ClaudeCliConfig config) {
        this.isRunning = false;
        this.config = config;
        this.useResume = true;
        this.usePtyMode = true;
        this.currentSessionId = null;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "SessionAwareClaudeExecutor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 设置消息回调接口
     *
     * @param callback 消息回调接口
     */
    public void setMessageCallback(MessageCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置Claude CLI配置
     *
     * @param config Claude CLI配置
     */
    public void setConfig(ClaudeCliConfig config) {
        this.config = config;
    }

    /**
     * 获取Claude CLI配置
     *
     * @return Claude CLI配置
     */
    public ClaudeCliConfig getConfig() {
        return config;
    }

    /**
     * 设置是否使用resume功能
     *
     * @param useResume 是否使用resume
     */
    public void setUseResume(boolean useResume) {
        this.useResume = useResume;
    }

    /**
     * 设置是否使用Pty模式
     *
     * @param usePtyMode 是否使用Pty模式
     */
    public void setUsePtyMode(boolean usePtyMode) {
        this.usePtyMode = usePtyMode;
    }

    /**
     * 获取是否使用Pty模式
     *
     * @return 是否使用Pty模式
     */
    public boolean isUsePtyMode() {
        return usePtyMode;
    }

    /**
     * 获取当前Claude状态（仅Pty模式）
     *
     * @return Claude状态
     */
    public ClaudeState getCurrentState() {
        if (usePtyMode && ptyManager != null) {
            return ptyManager.getCurrentState();
        }
        return null;
    }

    /**
     * 发送中断信号（仅Pty模式）
     */
    public void sendInterrupt() {
        if (usePtyMode && ptyManager != null) {
            ptyManager.sendInterrupt();
            logInfo("已发送中断信号");
        }
    }

    /**
     * 设置当前会话ID
     *
     * @param sessionId 会话ID
     */
    public void setCurrentSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    /**
     * 获取当前会话ID
     *
     * @return 会话ID
     */
    public String getCurrentSessionId() {
        return currentSessionId;
    }

    /**
     * 启动Claude CLI进程
     *
     * @throws Exception 如果启动失败
     */
    public void startClaudeProcess() throws Exception {
        if (isRunning) {
            throw new IllegalStateException("Claude进程已经在运行中");
        }

        try {
            if (usePtyMode) {
                // 优先尝试Pty模式，失败后回退到SDK模式
                try {
                    startPtyMode();
                } catch (Exception ptyError) {
                    logError("Pty模式启动失败，将回退到SDK模式: " + ptyError.getMessage());
                    startSdkMode();
                }
            } else {
                // 直接使用SDK模式
                startSdkMode();
            }

        } catch (Exception e) {
            isRunning = false;
            if (callback != null) {
                callback.onError("启动Claude失败: " + e.getMessage());
                callback.onConnectionStateChanged(false);
            }
            throw e;
        }
    }

    /**
     * 启动Pty模式
     */
    private void startPtyMode() throws Exception {
        logInfo("启动Pty模式Claude CLI...");

        // 创建PtyManager
        ptyManager = new PtyManager();

        // 设置回调
        ptyManager.setOutputListener(this::handlePtyOutput);
        ptyManager.setStateChangeListener(this::handlePtyStateChange);
        ptyManager.setErrorListener(this::handlePtyError);

        // 启动伪终端 - 针对Windows环境优化命令
        String[] command;
        String os = System.getProperty("os.name").toLowerCase();
        String claudeCmd = config.getClaudeCommand();

        if (os.contains("windows")) {
            // Windows环境下使用cmd包装器启动，确保正确的环境变量和路径处理
            if (claudeCmd.contains("npx")) {
                // 对于npx claude命令
                command = new String[]{"cmd", "/c", claudeCmd};
            } else if (claudeCmd.endsWith(".bat") || claudeCmd.endsWith(".cmd")) {
                // 对于批处理文件
                command = new String[]{"cmd", "/c", "\"" + claudeCmd + "\""};
            } else {
                // 对于普通exe或命令
                command = new String[]{"cmd", "/c", claudeCmd};
            }
            logInfo("Windows环境，使用包装命令: " + String.join(" ", command));
        } else {
            // Unix/Linux/Mac环境
            command = new String[]{claudeCmd};
            logInfo("Unix环境，使用直接命令: " + claudeCmd);
        }
        // 为非交互使用增加输出参数：json-stream（按次交互）
        try {
            String[] extended = new String[command.length + 3];
            System.arraycopy(command, 0, extended, 0, command.length);
            extended[command.length] = "--output-format";
            extended[command.length + 1] = "json-stream";
            extended[command.length + 2] = "--stream";
            command = extended;
        } catch (Exception ignore) {
            // 兼容性追加失败时忽略，保持原命令
        }

        try {
            ptyManager.startPty(command);

            // 等待一小段时间让进程启动
            Thread.sleep(1000);

            // 检查进程状态
            if (ptyManager.getCurrentState() == null) {
                logInfo("警告：Pty启动后状态为null，可能需要更多时间初始化");
            } else {
                logInfo("Pty状态: " + ptyManager.getCurrentState());
            }

            // 尝试发送一个简单的命令来激活Claude CLI
            // 这有助于确保Claude CLI完全就绪
            logInfo("发送初始化命令以激活Claude CLI...");
            ptyManager.sendCommand(""); // 发送空命令来触发提示符

            // 再等待一段时间让Claude CLI完全就绪
            Thread.sleep(2000);

            // 再次检查状态
            if (ptyManager.getCurrentState() != null) {
                logInfo("初始化后Pty状态: " + ptyManager.getCurrentState());
            }

        } catch (Exception e) {
            logError("启动Pty失败: " + e.getMessage());
            throw new Exception("无法启动Pty模式: " + e.getMessage(), e);
        }

        isRunning = true;

        // 通知回调
        if (callback != null) {
            callback.onProcessStarted();
            callback.onConnectionStateChanged(true);
            String message = "Claude CLI已启动（Pty模式）";
            if (useResume && currentSessionId != null) {
                message += "\n将在伪终端中保持会话连续性";
            }
            callback.onMessageReceived(message);
        }

        logInfo("Pty模式Claude CLI启动成功");
    }

    /**
     * 启动SDK模式（回退方案）
     */
    private void startSdkMode() throws Exception {
        logInfo("启动SDK模式...");

        // 测试类加载
        Class<?> sdkClass = Class.forName("com.anthropic.claude.client.ClaudeCodeSDK");
        logInfo("成功加载SDK类: " + sdkClass.getName());

        // 创建配置选项
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
            .authProvider(new DefaultAuthenticationProvider("local-cli-mode"))
            .cliMode(CliMode.PTY_INTERACTIVE)  // 设置为PTY交互模式
            .ptyReadyTimeout(Duration.ofSeconds(15))  // 可选：设置PTY就绪超时
            .cliEnabled(true)
            .enableLogging(false)
            .build();

        // 初始化Claude SDK
        claudeSDK = new ClaudeCodeSDK(options);
        isRunning = true;

        // 通知回调
        if (callback != null) {
            callback.onProcessStarted();
            callback.onConnectionStateChanged(true);
            String message = "Claude SDK已初始化（SDK模式），版本: " + claudeSDK.getVersion();
            if (useResume && currentSessionId != null) {
                message += "\n当前会话ID: " + currentSessionId;
            }
            callback.onMessageReceived(message);
        }

        logInfo("SDK模式启动成功");
    }

    /**
     * 处理Pty输出
     */
    private void handlePtyOutput(ClaudeResponse response) {
        if (callback == null) {
            return;
        }

        switch (response.getType()) {
            case TEXT_RESPONSE:
            case STREAM_CHUNK:
                callback.onMessageReceived(response.getContent());
                break;
            case ERROR_RESPONSE:
                callback.onError(response.getContent());
                break;
            case SESSION_INFO:
                handleSessionInfo(response);
                break;
            case STATUS_UPDATE:
                callback.onMessageReceived(response.getContent());
                break;
            case JSON_RESPONSE:
                // 可以进一步解析JSON响应
                callback.onMessageReceived(response.getContent());
                break;
            case SYSTEM_MESSAGE:
                logInfo("系统消息: " + response.getContent());
                break;
        }
    }

    /**
     * 处理Pty状态变化
     */
    private void handlePtyStateChange(StateChange stateChange) {
        ClaudeState newState = stateChange.getToState();
        logInfo("Claude状态变化: " + stateChange);

        // 根据状态变化执行相应操作
        switch (newState) {
            case READY:
                logInfo("Claude CLI已就绪，可以接收命令");
                break;
            case PROCESSING:
                logInfo("Claude正在处理请求...");
                break;
            case ERROR:
                logError("Claude出现错误状态");
                break;
            case USAGE_LIMIT:
                if (callback != null) {
                    callback.onError("达到使用限制，请稍后重试");
                }
                break;
            case AUTH_REQUIRED:
                if (callback != null) {
                    callback.onError("需要认证，请检查API密钥配置");
                }
                break;
            case SESSION_RESTORED:
                logInfo("会话已恢复");
                break;
        }
    }

    /**
     * 处理Pty错误
     */
    private void handlePtyError(String error) {
        logError("Pty错误: " + error);
        if (callback != null) {
            callback.onError("终端错误: " + error);
        }
    }

    /**
     * 处理会话信息
     */
    private void handleSessionInfo(ClaudeResponse response) {
        String sessionId = response.getSessionId();
        if (sessionId != null && !sessionId.equals(currentSessionId)) {
            String oldSessionId = currentSessionId;
            currentSessionId = sessionId;
            logInfo("会话ID更新: " + oldSessionId + " -> " + sessionId);
        }

        if (callback != null) {
            callback.onMessageReceived(response.getContent());
        }
    }

    /**
     * 发送命令到Claude CLI
     *
     * @param command 要发送的命令
     */
    public void sendCommand(String command) {
        if (!isRunning) {
            if (callback != null) {
                callback.onError("Claude未运行，无法发送命令");
            }
            return;
        }

        if (usePtyMode && ptyManager != null) {
            // 使用Pty模式发送命令
            sendCommandPtyMode(command);
        } else if (claudeSDK != null) {
            // 使用SDK模式发送命令
            sendCommandSdkMode(command);
        } else {
            if (callback != null) {
                callback.onError("未初始化任何Claude交互模式");
            }
        }
    }

    /**
     * Pty模式发送命令
     */
    private void sendCommandPtyMode(String command) {
        logInfo("发送命令(Pty模式): " + command);

        // 确保命令以换行符结尾，这对交互式CLI很重要
        String commandWithNewline = command.trim() + "\n";

        ptyManager.sendCommand(commandWithNewline)
            .thenAccept(response -> {
                logInfo("命令已发送到伪终端");
                // 响应将通过回调处理
            })
            .exceptionally(throwable -> {
                logError("发送命令失败: " + throwable.getMessage());
                if (callback != null) {
                    callback.onError("发送命令失败: " + throwable.getMessage());
                }
                return null;
            });
    }

    /**
     * SDK模式发送命令
     */
    private void sendCommandSdkMode(String command) {
        // 在后台线程中执行查询
        executorService.submit(() -> {
            try {
                // 构建查询请求，支持会话功能
                QueryRequest.Builder requestBuilder = QueryRequest.builder(command);

                // 如果启用resume功能，始终使用--continue来保持会话连续性
                if (useResume) {
                    requestBuilder.withContinueLastSession(true);
                    logInfo("使用自动会话继续模式，当前GUI会话: " + currentSessionId);
                }

                QueryRequest request = requestBuilder.build();

                // 执行查询
                CompletableFuture<Stream<Message>> future = claudeSDK.query(request);

                // 处理响应
                future.thenAccept(messageStream -> {
                    try {
                        messageStream.forEach(message -> {
                            if (callback != null) {
                                callback.onMessageReceived(message.getContent());
                            }
                        });
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onError("处理响应时出错: " + e.getMessage());
                        }
                    }
                }).exceptionally(throwable -> {
                    if (callback != null) {
                        callback.onError("查询执行失败: " + throwable.getMessage());
                    }
                    return null;
                });

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("发送命令失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 启动新会话
     *
     * @param sessionId GUI管理的会话ID
     */
    public void startNewSession(String sessionId) {
        this.currentSessionId = sessionId;

        if (callback != null) {
            String message = "开始新会话: " + sessionId;
            if (useResume) {
                message += "\n已启用会话保持功能，将使用Claude CLI内置会话管理";
            }
            callback.onMessageReceived(message);
        }

        logInfo("启动新会话: " + sessionId + "（GUI会话ID）");
    }

    /**
     * 恢复会话
     *
     * @param sessionId GUI管理的会话ID
     */
    public void resumeSession(String sessionId) {
        String previousSessionId = this.currentSessionId;

        // 如果是相同的会话ID，避免重复处理
        if (sessionId != null && sessionId.equals(previousSessionId)) {
            return;
        }

        this.currentSessionId = sessionId;

        if (callback != null) {
            String message;
            if (previousSessionId != null && !previousSessionId.equals(sessionId)) {
                message = "从会话 \"" + previousSessionId + "\" 切换到 \"" + sessionId + "\"";
            } else if (previousSessionId == null) {
                message = "恢复会话: " + sessionId;
            } else {
                // 相同会话，不发送消息
                return;
            }
            if (useResume) {
                message += "\n将使用Claude CLI内置会话管理进行会话恢复";
            }
            callback.onMessageReceived(message);
        }

        logInfo("切换到会话: " + sessionId + " (从: " + previousSessionId + ")");
    }

    /**
     * 停止Claude进程
     */
    public void stopProcess() {
        if (!isRunning) {
            return;
        }

        logInfo("停止Claude进程...");
        isRunning = false;

        // 清空会话信息
        currentSessionId = null;

        // 关闭PtyManager
        if (ptyManager != null) {
            try {
                ptyManager.closePty();
                logInfo("Pty进程已关闭");
            } catch (Exception e) {
                logError("关闭Pty进程时出错: " + e.getMessage());
            }
            ptyManager = null;
        }

        // 关闭SDK
        if (claudeSDK != null) {
            try {
                claudeSDK.shutdown();
                logInfo("Claude SDK已关闭");
            } catch (Exception e) {
                logError("关闭Claude SDK时出错: " + e.getMessage());
            }
            claudeSDK = null;
        }

        // 关闭执行器
        if (executorService != null) {
            executorService.shutdown();
        }

        // 通知回调已关闭
        if (callback != null) {
            callback.onConnectionStateChanged(false);
            callback.onProcessFinished();
        }

        logInfo("Claude进程已停止");
    }

    /**
     * 检查Claude是否正在运行
     *
     * @return 如果Claude正在运行返回true，否则返回false
     */
    public boolean isRunning() {
        if (!isRunning) {
            return false;
        }

        if (usePtyMode && ptyManager != null) {
            return ptyManager.isAlive();
        } else if (claudeSDK != null) {
            return claudeSDK.healthCheck();
        }

        return false;
    }

    /**
     * 获取SDK版本信息
     *
     * @return SDK版本信息
     */
    public String getVersion() {
        if (claudeSDK != null) {
            return claudeSDK.getVersion();
        }
        return "未知";
    }

    /**
     * 检查是否有活跃会话
     *
     * @return 是否有活跃会话
     */
    public boolean hasActiveSession() {
        return currentSessionId != null;
    }

    /**
     * 安全的信息日志输出方法，处理编码问题
     *
     * @param message 日志消息
     */
    private void logInfo(String message) {
        try {
            // 使用UTF-8编码输出
            byte[] bytes = message.getBytes("UTF-8");
            System.out.write(bytes);
            System.out.println();
            System.out.flush();
        } catch (Exception e) {
            // 如果UTF-8输出失败，使用默认方式
            System.out.println(message);
        }
    }

    /**
     * 安全的错误日志输出方法，处理编码问题
     *
     * @param message 错误消息
     */
    private void logError(String message) {
        try {
            // 使用UTF-8编码输出
            byte[] bytes = message.getBytes("UTF-8");
            System.err.write(bytes);
            System.err.println();
            System.err.flush();
        } catch (Exception e) {
            // 如果UTF-8输出失败，使用默认方式
            System.err.println(message);
        }
    }
}
