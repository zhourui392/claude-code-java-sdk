package com.anthropic.claude.client;

import com.anthropic.claude.config.ClaudeAgentOptions;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.config.ConfigLoader;
import com.anthropic.claude.hooks.HookCallback;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.query.QueryBuilder;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.subagents.SubagentManager;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Claude Code SDK (已弃用)
 *
 * <p><strong>⚠️ 已弃用：</strong>此类已在 v2.0.0 中弃用。请改用 {@link ClaudeAgentSDK}。</p>
 *
 * <p>此类现在作为 {@link ClaudeAgentSDK} 的委托包装器存在，以保持向后兼容性。
 * 所有方法调用都委托给底层的 ClaudeAgentSDK 实例。</p>
 *
 * <h3>迁移指南</h3>
 * <pre>{@code
 * // v1.0.0 (已弃用)
 * ClaudeCodeSDK sdk = new ClaudeCodeSDK();
 * ClaudeCodeOptions options = ClaudeCodeOptions.builder()
 *     .apiKey("...")
 *     .build();
 *
 * // v2.0.0 (推荐)
 * ClaudeAgentSDK sdk = new ClaudeAgentSDK();
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .apiKey("...")
 *     .build();
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 1.0.0
 * @deprecated 自 2.0.0 起弃用，请使用 {@link ClaudeAgentSDK}
 */
@Deprecated
public class ClaudeCodeSDK {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeSDK.class);
    private static final AtomicBoolean deprecationWarningLogged = new AtomicBoolean(false);

    // 委托给新的 ClaudeAgentSDK
    private final ClaudeAgentSDK delegate;
    private final ClaudeCodeOptions legacyOptions;

    /**
     * 使用默认配置创建 SDK 实例
     *
     * @deprecated 使用 {@link ClaudeAgentSDK#ClaudeAgentSDK()}
     */
    @Deprecated
    public ClaudeCodeSDK() {
        this(new ConfigLoader().createOptions());
    }

    /**
     * 使用自定义配置创建 SDK 实例
     *
     * @param options 配置选项
     * @deprecated 使用 {@link ClaudeAgentSDK#ClaudeAgentSDK(ClaudeAgentOptions)}
     */
    @Deprecated
    public ClaudeCodeSDK(ClaudeCodeOptions options) {
        logDeprecationWarning();

        this.legacyOptions = options;
        ClaudeAgentOptions agentOptions = convertToAgentOptions(options);
        this.delegate = new ClaudeAgentSDK(agentOptions);

        logger.debug("ClaudeCodeSDK 委托包装器已初始化");
    }

    // ====== 查询方法 ======

    /**
     * 执行查询
     *
     * @param prompt 查询提示
     * @return 消息流的 CompletableFuture
     * @deprecated 使用 {@link ClaudeAgentSDK#query(String)}
     */
    @Deprecated
    public CompletableFuture<Stream<Message>> query(String prompt) {
        return delegate.query(prompt);
    }

    /**
     * 执行查询（完整参数）
     *
     * @param request 查询请求
     * @return 消息流的 CompletableFuture
     * @deprecated 使用 {@link ClaudeAgentSDK#query(QueryRequest)}
     */
    @Deprecated
    public CompletableFuture<Stream<Message>> query(QueryRequest request) {
        return delegate.query(request);
    }

    /**
     * 执行流式查询
     *
     * @param prompt 查询提示
     * @return 消息的 Observable 流
     * @deprecated 使用 {@link ClaudeAgentSDK#queryStream(String)}
     */
    @Deprecated
    public Observable<Message> queryStream(String prompt) {
        return delegate.queryStream(prompt);
    }

    /**
     * 执行流式查询（完整参数）
     *
     * @param request 查询请求
     * @return 消息的 Observable 流
     * @deprecated 使用 {@link ClaudeAgentSDK#queryStream(QueryRequest)}
     */
    @Deprecated
    public Observable<Message> queryStream(QueryRequest request) {
        return delegate.queryStream(request);
    }

    /**
     * 创建查询构建器
     *
     * @param prompt 初始提示
     * @return 查询构建器
     * @deprecated 使用 {@link ClaudeAgentSDK#queryBuilder(String)}
     */
    @Deprecated
    public QueryBuilder queryBuilder(String prompt) {
        return delegate.queryBuilder(prompt);
    }

    // ====== Hook 管理 ======

    /**
     * 添加事件钩子
     *
     * @param eventType 事件类型
     * @param callback  回调函数
     * @deprecated 使用 {@link ClaudeAgentSDK#addHook(String, HookCallback)}
     */
    @Deprecated
    public void addHook(String eventType, HookCallback callback) {
        delegate.addHook(eventType, callback);
    }

    /**
     * 移除事件钩子
     *
     * @param eventType 事件类型
     * @param callback  回调函数
     * @deprecated 使用 {@link ClaudeAgentSDK#removeHook(String, HookCallback)}
     */
    @Deprecated
    public void removeHook(String eventType, HookCallback callback) {
        delegate.removeHook(eventType, callback);
    }

    // ====== 管理器访问 ======

    /**
     * 获取子代理管理器
     *
     * @return 子代理管理器
     * @deprecated 使用 {@link ClaudeAgentSDK#getSubagentManager()}
     */
    @Deprecated
    public SubagentManager getSubagentManager() {
        return delegate.getSubagentManager();
    }

    /**
     * 运行时配置更改（未实现）
     *
     * @param newOptions 新配置
     * @deprecated 该功能已移除，请创建新的 SDK 实例
     */
    @Deprecated
    public void configure(ClaudeCodeOptions newOptions) {
        logger.warn("运行时配置更改尚未实现");
        throw new UnsupportedOperationException("运行时配置更改尚未实现");
    }

    /**
     * 获取当前配置
     *
     * @return 配置选项
     * @deprecated 使用 {@link ClaudeAgentSDK#getConfiguration()}
     */
    @Deprecated
    public ClaudeCodeOptions getConfiguration() {
        return legacyOptions;
    }

    // ====== 认证和健康检查 ======

    /**
     * 检查认证状态
     *
     * @return 是否已认证
     * @deprecated 使用 {@link ClaudeAgentSDK#isAuthenticated()}
     */
    @Deprecated
    public boolean isAuthenticated() {
        return delegate.isAuthenticated();
    }

    /**
     * 刷新认证令牌
     *
     * @deprecated 使用 {@link ClaudeAgentSDK#refreshAuthentication()}
     */
    @Deprecated
    public void refreshAuthentication() {
        delegate.refreshAuthentication();
    }

    /**
     * 检查 CLI 可用性
     *
     * @return CLI 是否可用
     * @deprecated 使用 {@link ClaudeAgentSDK#isCliAvailable()}
     */
    @Deprecated
    public boolean isCliAvailable() {
        return delegate.isCliAvailable();
    }

    /**
     * 关闭 SDK 并释放资源
     *
     * @deprecated 使用 {@link ClaudeAgentSDK#shutdown()}
     */
    @Deprecated
    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * 获取 SDK 版本
     *
     * @return 版本号（返回 "1.0.0" 以保持兼容）
     * @deprecated 使用 {@link ClaudeAgentSDK#getVersion()}
     */
    @Deprecated
    public String getVersion() {
        return "1.0.0";
    }

    /**
     * 执行健康检查
     *
     * @return 是否健康
     * @deprecated 使用 {@link ClaudeAgentSDK#healthCheck()}
     */
    @Deprecated
    public boolean healthCheck() {
        return delegate.healthCheck();
    }

    // ====== 内部辅助方法 ======

    /**
     * 将 ClaudeCodeOptions 转换为 ClaudeAgentOptions
     *
     * @param codeOptions 旧配置
     * @return 新配置
     */
    private ClaudeAgentOptions convertToAgentOptions(ClaudeCodeOptions codeOptions) {
        ClaudeAgentOptions.Builder builder = ClaudeAgentOptions.builder()
            .apiKey(codeOptions.getApiKey())
            .baseUrl(codeOptions.getBaseUrl())
            .cliPath(codeOptions.getCliPath())
            .cliEnabled(codeOptions.isCliEnabled())
            .timeout(codeOptions.getTimeout())
            .maxRetries(codeOptions.getMaxRetries())
            .enableLogging(codeOptions.isEnableLogging())
            .environment(codeOptions.getEnvironment())
            .cliMode(codeOptions.getCliMode())
            .ptyReadyTimeout(codeOptions.getPtyReadyTimeout())
            .promptPattern(codeOptions.getPromptPattern())
            .additionalArgs(codeOptions.getAdditionalArgs())
            .minPoolSize(codeOptions.getMinPoolSize())
            .maxPoolSize(codeOptions.getMaxPoolSize())
            .connectionTimeout(codeOptions.getConnectionTimeout())
            .healthCheckInterval(codeOptions.getHealthCheckInterval());

        // 可选字段
        if (codeOptions.getAuthProvider() != null) {
            builder.authProvider(codeOptions.getAuthProvider());
        }

        // 使用 v1.x 兼容模式（加载所有配置源）
        builder.useV1CompatibilityMode();

        // 抑制弃用警告（因为这本身就是弃用类）
        builder.suppressDeprecationWarnings(true);

        return builder.build();
    }

    /**
     * 记录弃用警告（每个进程仅记录一次）
     */
    private void logDeprecationWarning() {
        if (deprecationWarningLogged.compareAndSet(false, true)) {
            logger.warn("⚠️  ClaudeCodeSDK 已在 v2.0.0 中弃用，请迁移到 ClaudeAgentSDK");
            logger.warn("   迁移指南: https://github.com/yourproject/docs/migration-guide-v2.md");
            logger.warn("   此兼容层将在 v3.0.0 中移除");
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ClaudeCodeSDK{version=%s, delegate=%s} [DEPRECATED]",
            getVersion(), delegate
        );
    }
}
