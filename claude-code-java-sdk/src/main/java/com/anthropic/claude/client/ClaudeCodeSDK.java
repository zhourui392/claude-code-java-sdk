package com.anthropic.claude.client;

import com.anthropic.claude.auth.AuthenticationProvider;
import com.anthropic.claude.auth.DefaultAuthenticationProvider;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.config.ConfigLoader;
import com.anthropic.claude.hooks.HookCallback;
import com.anthropic.claude.hooks.HookService;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.process.ProcessManager;
import com.anthropic.claude.query.QueryBuilder;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.query.QueryService;
import com.anthropic.claude.subagents.SubagentManager;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ClaudeCodeSDK {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeSDK.class);

    private final ClaudeCodeOptions options;
    private final ConfigLoader configLoader;
    private final ProcessManager processManager;
    private final QueryService queryService;
    private final HookService hookService;
    private final SubagentManager subagentManager;
    private final AuthenticationProvider authProvider;

    public ClaudeCodeSDK() {
        this(new ConfigLoader().createOptions());
    }

    public ClaudeCodeSDK(ClaudeCodeOptions options) {
        this.options = options;
        this.configLoader = new ConfigLoader();

        try {
            configLoader.validateConfiguration();
        } catch (Exception e) {
            logger.warn("配置验证失败: {}", e.getMessage());
        }

        this.authProvider = options.getAuthProvider() != null
                ? options.getAuthProvider()
                : new DefaultAuthenticationProvider(options.getApiKey());

        this.processManager = new ProcessManager(options.getTimeout(), options.getEnvironment());
        this.hookService = new HookService();
        this.queryService = new QueryService(processManager, hookService, options);
        this.subagentManager = new SubagentManager(processManager, options);

        logger.info("Claude Code SDK 初始化完成");
    }

    public CompletableFuture<Stream<Message>> query(String prompt) {
        return query(QueryRequest.builder(prompt).build());
    }

    public CompletableFuture<Stream<Message>> query(QueryRequest request) {
        logger.debug("执行查询: {}", request.getPrompt());
        return queryService.queryAsync(request);
    }

    public Observable<Message> queryStream(String prompt) {
        return queryStream(QueryRequest.builder(prompt).build());
    }

    public Observable<Message> queryStream(QueryRequest request) {
        logger.debug("执行流式查询: {}", request.getPrompt());
        return queryService.queryStream(request);
    }

    public QueryBuilder queryBuilder(String prompt) {
        return new QueryBuilder(prompt, queryService);
    }

    public void addHook(String eventType, HookCallback callback) {
        hookService.addHook(eventType, callback);
        logger.debug("添加Hook: {}", eventType);
    }

    public void removeHook(String eventType, HookCallback callback) {
        hookService.removeHook(eventType, callback);
        logger.debug("移除Hook: {}", eventType);
    }

    public SubagentManager getSubagentManager() {
        return subagentManager;
    }

    public void configure(ClaudeCodeOptions newOptions) {
        logger.warn("运行时配置更改尚未实现");
        throw new UnsupportedOperationException("运行时配置更改尚未实现");
    }

    public ClaudeCodeOptions getConfiguration() {
        return options;
    }

    public boolean isAuthenticated() {
        return authProvider.isAuthenticated();
    }

    public void refreshAuthentication() {
        authProvider.refreshAuth();
        logger.debug("认证已刷新");
    }

    public boolean isCliAvailable() {
        return true;
    }

    public void shutdown() {
        try {
            subagentManager.shutdown();
            logger.info("Claude Code SDK 已关闭");
        } catch (Exception e) {
            logger.error("关闭SDK时出错", e);
        }
    }

    public String getVersion() {
        return "1.0.0";
    }

    public boolean healthCheck() {
        try {
            return isAuthenticated() && isCliAvailable();
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            return false;
        }
    }
}

