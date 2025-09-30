package com.claude.gui.executor;

import com.anthropic.claude.config.CliMode;
import com.claude.gui.callback.MessageCallback;
import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.auth.DefaultAuthenticationProvider;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.messages.Message;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Claude CLI执行器类，使用Claude Code Java SDK进行交互
 * 替代了直接进程调用的方式，使用SDK的API进行通信
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class ClaudeCliExecutor {

    private ClaudeCodeSDK claudeSDK;
    private MessageCallback callback;
    private volatile boolean isRunning;
    private ClaudeCliConfig config;
    private ExecutorService executorService;

    /**
     * 构造函数
     */
    public ClaudeCliExecutor() {
        this.isRunning = false;
        this.config = new ClaudeCliConfig();
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ClaudeSDK-Executor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 构造函数
     *
     * @param config Claude CLI配置
     */
    public ClaudeCliExecutor(ClaudeCliConfig config) {
        this.isRunning = false;
        this.config = config;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ClaudeSDK-Executor");
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
     * 启动Claude SDK
     *
     * @throws Exception 如果SDK启动失败
     */
    public void startClaudeProcess() throws Exception {
        if (isRunning) {
            throw new IllegalStateException("Claude SDK已经在运行中");
        }

        try {
            // 测试类加载
            logInfo("尝试加载ClaudeCodeSDK类...");
            Class<?> sdkClass = Class.forName("com.anthropic.claude.client.ClaudeCodeSDK");
            logInfo("成功加载SDK类: " + sdkClass.getName());

            // 创建配置选项，提供认证提供者用于本地CLI模式
            ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .authProvider(new DefaultAuthenticationProvider("local-cli-mode"))
                .cliMode(CliMode.PTY_INTERACTIVE)  // 设置为PTY交互模式
                .ptyReadyTimeout(Duration.ofSeconds(15))  // 可选：设置PTY就绪超时
                .cliEnabled(true)
                .enableLogging(false)
                .build();

            // 初始化Claude SDK
            claudeSDK = new ClaudeCodeSDK(options);

            // 跳过健康检查，因为我们使用本地CLI模式
            // if (!claudeSDK.healthCheck()) {
            //     throw new Exception("Claude SDK健康检查失败");
            // }

            isRunning = true;

            // 通知回调SDK已启动
            if (callback != null) {
                callback.onProcessStarted();
                callback.onConnectionStateChanged(true);
                callback.onMessageReceived("Claude SDK已初始化（本地CLI模式），版本: " + claudeSDK.getVersion());
            }

        } catch (Exception e) {
            isRunning = false;
            if (callback != null) {
                callback.onError("启动Claude SDK失败: " + e.getMessage());
                callback.onConnectionStateChanged(false);
            }
            throw e;
        }
    }

    /**
     * 发送命令到Claude SDK
     *
     * @param command 要发送的命令
     */
    public void sendCommand(String command) {
        if (!isRunning || claudeSDK == null) {
            if (callback != null) {
                callback.onError("Claude SDK未运行，无法发送命令");
            }
            return;
        }

        // 在后台线程中执行查询
        executorService.submit(() -> {
            try {
                // 构建查询请求
                QueryRequest request = QueryRequest.builder(command).build();

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
     * 停止Claude SDK
     */
    public void stopProcess() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        // 关闭SDK
        if (claudeSDK != null) {
            try {
                claudeSDK.shutdown();
            } catch (Exception e) {
                logError("关闭Claude SDK时出错: " + e.getMessage());
            }
        }

        // 关闭执行器
        if (executorService != null) {
            executorService.shutdown();
        }

        // 通知回调SDK已关闭
        if (callback != null) {
            callback.onConnectionStateChanged(false);
            callback.onProcessFinished();
        }
    }

    /**
     * 检查SDK是否正在运行
     *
     * @return 如果SDK正在运行返回true，否则返回false
     */
    public boolean isRunning() {
        return isRunning && claudeSDK != null && claudeSDK.healthCheck();
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