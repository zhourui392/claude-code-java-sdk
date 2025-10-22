package com.anthropic.claude.client;

import com.anthropic.claude.auth.AuthenticationProvider;
import com.anthropic.claude.auth.DefaultAuthenticationProvider;
import com.anthropic.claude.config.ClaudeAgentOptions;
import com.anthropic.claude.config.CliMode;
import com.anthropic.claude.config.ConfigLoader;
import com.anthropic.claude.hooks.HookCallback;
import com.anthropic.claude.hooks.HookService;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.process.ProcessManager;
import com.anthropic.claude.pty.PtyManager;
import com.anthropic.claude.query.QueryBuilder;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.query.QueryService;
import com.anthropic.claude.subagents.SubagentManager;
import com.anthropic.claude.transport.ClaudeTransport;
import com.anthropic.claude.transport.ProcessTransport;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Claude Agent SDK 主入口类
 *
 * <p>提供与 Claude Agent CLI 交互的高级 API。</p>
 *
 * <h3>基础使用</h3>
 * <pre>{@code
 * ClaudeAgentSDK sdk = new ClaudeAgentSDK();
 *
 * // 同步查询
 * CompletableFuture<Stream<Message>> future = sdk.query("What is 2+2?");
 * future.thenAccept(messages -> messages.forEach(System.out::println));
 *
 * // 流式查询
 * sdk.queryStream("Explain quantum computing")
 *     .subscribe(message -> System.out.println(message));
 * }</pre>
 *
 * <h3>高级功能</h3>
 * <pre>{@code
 * // 自定义配置
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .apiKey(System.getenv("ANTHROPIC_API_KEY"))
 *     .timeout(Duration.ofMinutes(10))
 *     .settingSources(SettingSource.ENV_VARS, SettingSource.USER_CONFIG)
 *     .build();
 *
 * ClaudeAgentSDK sdk = new ClaudeAgentSDK(options);
 *
 * // 添加钩子
 * sdk.addHook("pre_query", context -> {
 *     System.out.println("Query: " + context.getData().get("prompt"));
 *     return HookResult.approve();
 * });
 *
 * // 使用子代理
 * String agentId = sdk.getSubagentManager()
 *     .startInlineSubagent("code-reviewer", context);
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClaudeAgentSDK {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeAgentSDK.class);

    private final ClaudeAgentOptions options;
    private final ConfigLoader configLoader;
    private final ClaudeTransport transport;
    private final QueryService queryService;
    private final HookService hookService;
    private final SubagentManager subagentManager;
    private final SessionManager sessionManager;
    private final AuthenticationProvider authProvider;

    /**
     * 使用默认配置创建 SDK 实例
     */
    public ClaudeAgentSDK() {
        this(createDefaultOptions());
    }

    /**
     * 使用自定义配置创建 SDK 实例
     *
     * @param options 配置选项
     */
    public ClaudeAgentSDK(ClaudeAgentOptions options) {
        this.options = Objects.requireNonNull(options, "options cannot be null");
        this.configLoader = new ConfigLoader();

        // 配置验证
        try {
            configLoader.validateConfiguration();
        } catch (Exception e) {
            logger.warn("配置验证失败: {}", e.getMessage());
        }

        // 认证提供者初始化
        this.authProvider = initializeAuthProvider();

        // 传输层初始化
        this.transport = initializeTransport();

        // 核心服务初始化
        this.hookService = new HookService();
        this.queryService = initializeQueryService();
        this.subagentManager = initializeSubagentManager();
        this.sessionManager = new SessionManager(options);

        // 注册内联子代理
        if (!options.getAgents().isEmpty()) {
            logger.info("注册 {} 个内联子代理", options.getAgents().size());
            options.getAgents().values()
                .forEach(subagentManager::registerInlineAgent);
        }

        logger.info("Claude Agent SDK v{} 初始化完成", getVersion());
    }

    // ====== 查询方法 ======

    /**
     * 执行查询（异步）
     *
     * @param prompt 查询提示
     * @return 消息流的 CompletableFuture
     */
    public CompletableFuture<Stream<Message>> query(String prompt) {
        return query(QueryRequest.builder(prompt).build());
    }

    /**
     * 执行查询（异步，完整参数）
     *
     * @param request 查询请求
     * @return 消息流的 CompletableFuture
     */
    public CompletableFuture<Stream<Message>> query(QueryRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        logger.debug("执行查询: {}", request.getPrompt());
        return queryService.queryAsync(request);
    }

    /**
     * 执行流式查询（响应式）
     *
     * @param prompt 查询提示
     * @return 消息的 Observable 流
     */
    public Observable<Message> queryStream(String prompt) {
        return queryStream(QueryRequest.builder(prompt).build());
    }

    /**
     * 执行流式查询（响应式，完整参数）
     *
     * @param request 查询请求
     * @return 消息的 Observable 流
     */
    public Observable<Message> queryStream(QueryRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        logger.debug("执行流式查询: {}", request.getPrompt());
        return queryService.queryStream(request);
    }

    /**
     * 创建查询构建器
     *
     * @param prompt 初始提示
     * @return 查询构建器
     */
    public QueryBuilder queryBuilder(String prompt) {
        return new QueryBuilder(prompt, queryService);
    }

    // ====== Hook 管理 ======

    /**
     * 添加事件钩子
     *
     * @param eventType 事件类型（pre_query, post_query, query_error）
     * @param callback 回调函数
     */
    public void addHook(String eventType, HookCallback callback) {
        hookService.addHook(eventType, callback);
        logger.debug("添加Hook: {}", eventType);
    }

    /**
     * 移除事件钩子
     *
     * @param eventType 事件类型
     * @param callback 回调函数
     */
    public void removeHook(String eventType, HookCallback callback) {
        hookService.removeHook(eventType, callback);
        logger.debug("移除Hook: {}", eventType);
    }

    // ====== 管理器访问 ======

    /**
     * 获取子代理管理器
     *
     * @return 子代理管理器
     */
    public SubagentManager getSubagentManager() {
        return subagentManager;
    }

    /**
     * 获取会话管理器
     *
     * @return 会话管理器
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * 获取当前配置（只读）
     *
     * @return 配置选项
     */
    public ClaudeAgentOptions getConfiguration() {
        return options;
    }

    // ====== 认证和健康检查 ======

    /**
     * 检查认证状态
     *
     * @return 是否已认证
     */
    public boolean isAuthenticated() {
        return authProvider.isAuthenticated();
    }

    /**
     * 刷新认证令牌
     */
    public void refreshAuthentication() {
        authProvider.refreshAuth();
        logger.debug("认证已刷新");
    }

    /**
     * 检查 CLI 可用性
     *
     * @return CLI 是否可用
     */
    public boolean isCliAvailable() {
        return transport.isConnected();
    }

    /**
     * 执行健康检查
     *
     * @return 是否健康
     */
    public boolean healthCheck() {
        try {
            boolean authOk = isAuthenticated();
            boolean cliOk = isCliAvailable();
            boolean sessionOk = sessionManager.isHealthy();

            logger.debug("健康检查: auth={}, cli={}, session={}",
                authOk, cliOk, sessionOk);

            return authOk && cliOk && sessionOk;

        } catch (Exception e) {
            logger.error("健康检查失败", e);
            return false;
        }
    }

    // ====== 生命周期管理 ======

    /**
     * 关闭 SDK 并释放资源
     */
    public void shutdown() {
        try {
            logger.info("正在关闭 Claude Agent SDK...");

            // 关闭子代理
            subagentManager.shutdown();

            // 关闭会话管理器
            sessionManager.shutdown();

            // 关闭传输层
            transport.close();

            logger.info("Claude Agent SDK 已关闭");

        } catch (Exception e) {
            logger.error("关闭 SDK 时出错", e);
        }
    }

    /**
     * 获取 SDK 版本
     *
     * @return 版本号
     */
    public String getVersion() {
        return "2.0.0";
    }

    // ====== 内部辅助方法 ======

    /**
     * 创建默认配置
     */
    private static ClaudeAgentOptions createDefaultOptions() {
        return new ConfigLoader().createAgentOptions();
    }

    /**
     * 初始化认证提供者
     */
    private AuthenticationProvider initializeAuthProvider() {
        if (options.getAuthProvider() != null) {
            return options.getAuthProvider();
        }

        return new DefaultAuthenticationProvider(options.getApiKey());
    }

    /**
     * 初始化传输层
     */
    private ClaudeTransport initializeTransport() {
        // 使用自定义传输层
        if (options.getCustomTransport() != null) {
            logger.info("使用自定义传输层: {}", options.getCustomTransport().getType());
            return options.getCustomTransport();
        }

        // 默认使用 CLI 进程传输
        ProcessManager processManager = new ProcessManager(
            options.getTimeout(),
            options.getEnvironment()
        );

        return new ProcessTransport(processManager, options);
    }

    /**
     * 初始化查询服务
     */
    private QueryService initializeQueryService() {
        ProcessManager processManager = new ProcessManager(
            options.getTimeout(),
            options.getEnvironment()
        );

        // 根据配置选择执行模式
        if (options.getCliMode() == CliMode.PTY_INTERACTIVE) {
            logger.info("初始化 QueryService（PTY 交互模式）");
            return new QueryService(processManager, new PtyManager(), hookService, options);
        } else {
            logger.info("初始化 QueryService（批处理模式）");
            return new QueryService(processManager, hookService, options);
        }
    }

    /**
     * 初始化子代理管理器
     */
    private SubagentManager initializeSubagentManager() {
        ProcessManager processManager = new ProcessManager(
            options.getTimeout(),
            options.getEnvironment()
        );

        return new SubagentManager(processManager, options);
    }

    @Override
    public String toString() {
        return String.format(
            "ClaudeAgentSDK{version=%s, authenticated=%s, transport=%s, agents=%d}",
            getVersion(), isAuthenticated(), transport.getType(), options.getAgents().size()
        );
    }
}
