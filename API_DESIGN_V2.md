# API 设计文档 - v2.0.0

> **版本**: v2.0.0
> **创建日期**: 2025-10-21
> **对标**: Claude Agent SDK Python v0.1.4

---

## 📚 目录

1. [核心类重命名](#1-核心类重命名)
2. [Session Forking API](#2-session-forking-api)
3. [Programmatic Subagents API](#3-programmatic-subagents-api)
4. [Settings Sources API](#4-settings-sources-api)
5. [Custom Transport API](#5-custom-transport-api)
6. [Enhanced Hook Fields API](#6-enhanced-hook-fields-api)
7. [向后兼容层设计](#7-向后兼容层设计)

---

## 1. 核心类重命名

### 1.1 ClaudeAgentSDK

**位置**: `com.anthropic.claude.client.ClaudeAgentSDK`

```java
package com.anthropic.claude.client;

/**
 * Claude Agent SDK 主入口类
 *
 * <p>提供与 Claude Agent CLI 交互的高级 API</p>
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
        this(new ConfigLoader().createOptions());
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
            configLoader.validateConfiguration(options);
        } catch (Exception e) {
            logger.warn("配置验证失败: {}", e.getMessage());
        }

        // 认证提供者
        this.authProvider = options.getAuthProvider() != null
                ? options.getAuthProvider()
                : new DefaultAuthenticationProvider(options.getApiKey());

        // 传输层初始化
        this.transport = initializeTransport(options);

        // 核心服务初始化
        this.hookService = new HookService();
        this.queryService = new QueryService(transport, hookService, options);
        this.subagentManager = new SubagentManager(transport, options);
        this.sessionManager = new SessionManager(options);

        // 注册内联子代理
        if (!options.getAgents().isEmpty()) {
            options.getAgents().values()
                .forEach(subagentManager::registerInlineAgent);
        }

        logger.info("Claude Agent SDK v{} 初始化完成", getVersion());
    }

    /**
     * 初始化传输层
     */
    private ClaudeTransport initializeTransport(ClaudeAgentOptions options) {
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

    /**
     * 添加事件钩子
     *
     * @param eventType 事件类型（pre_query, post_query, query_error）
     * @param callback 回调函数
     */
    public void addHook(String eventType, HookCallback callback) {
        hookService.addHook(eventType, callback);
        logger.debug("添加钩子: {}", eventType);
    }

    /**
     * 移除事件钩子
     *
     * @param eventType 事件类型
     * @param callback 回调函数
     */
    public void removeHook(String eventType, HookCallback callback) {
        hookService.removeHook(eventType, callback);
        logger.debug("移除钩子: {}", eventType);
    }

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
     * 获取当前配置
     *
     * @return 配置选项（只读）
     */
    public ClaudeAgentOptions getConfiguration() {
        return options;
    }

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
     * 关闭 SDK 并释放资源
     */
    public void shutdown() {
        try {
            sessionManager.shutdown();
            subagentManager.shutdown();
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

    /**
     * 健康检查
     *
     * @return 是否健康
     */
    public boolean healthCheck() {
        try {
            return isAuthenticated() && isCliAvailable();
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            return false;
        }
    }
}
```

---

### 1.2 ClaudeAgentOptions

**位置**: `com.anthropic.claude.config.ClaudeAgentOptions`

```java
package com.anthropic.claude.config;

/**
 * Claude Agent SDK 配置选项
 *
 * <p>使用 Builder 模式创建配置实例</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClaudeAgentOptions {

    // 基础配置
    private final String apiKey;
    private final String baseUrl;
    private final String cliPath;
    private final boolean cliEnabled;
    private final Duration timeout;
    private final int maxRetries;
    private final boolean enableLogging;
    private final Map<String, String> environment;
    private final AuthenticationProvider authProvider;

    // v2.0.0 新增配置
    private final List<SettingSource> settingSources;
    private final Map<String, SubagentDefinition> agents;
    private final ClaudeTransport customTransport;

    // CLI 模式配置
    private final CliMode cliMode;
    private final Duration ptyReadyTimeout;
    private final String promptPattern;
    private final List<String> additionalArgs;

    // 连接池配置
    private final int minPoolSize;
    private final int maxPoolSize;
    private final long connectionTimeout;
    private final long healthCheckInterval;

    private ClaudeAgentOptions(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.cliPath = builder.cliPath;
        this.cliEnabled = builder.cliEnabled;
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.enableLogging = builder.enableLogging;
        this.environment = Collections.unmodifiableMap(builder.environment);
        this.authProvider = builder.authProvider;
        this.settingSources = Collections.unmodifiableList(builder.settingSources);
        this.agents = Collections.unmodifiableMap(builder.agents);
        this.customTransport = builder.customTransport;
        this.cliMode = builder.cliMode;
        this.ptyReadyTimeout = builder.ptyReadyTimeout;
        this.promptPattern = builder.promptPattern;
        this.additionalArgs = Collections.unmodifiableList(builder.additionalArgs);
        this.minPoolSize = builder.minPoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.healthCheckInterval = builder.healthCheckInterval;
    }

    // Getters（所有字段）
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getCliPath() { return cliPath; }
    public boolean isCliEnabled() { return cliEnabled; }
    public Duration getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isEnableLogging() { return enableLogging; }
    public Map<String, String> getEnvironment() { return environment; }
    public AuthenticationProvider getAuthProvider() { return authProvider; }

    // v2.0.0 新增 Getters
    public List<SettingSource> getSettingSources() { return settingSources; }
    public Map<String, SubagentDefinition> getAgents() { return agents; }
    public ClaudeTransport getCustomTransport() { return customTransport; }

    public CliMode getCliMode() { return cliMode; }
    public Duration getPtyReadyTimeout() { return ptyReadyTimeout; }
    public String getPromptPattern() { return promptPattern; }
    public List<String> getAdditionalArgs() { return additionalArgs; }

    public int getMinPoolSize() { return minPoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getHealthCheckInterval() { return healthCheckInterval; }

    /**
     * 创建 Builder 实例
     *
     * @return 新的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ClaudeAgentOptions Builder
     */
    public static class Builder {
        // 默认值
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com";
        private String cliPath = ClaudePathResolver.resolveClaudePath();
        private boolean cliEnabled = false;
        private Duration timeout = Duration.ofMinutes(10);
        private int maxRetries = 3;
        private boolean enableLogging = true;
        private Map<String, String> environment = new HashMap<>();
        private AuthenticationProvider authProvider;

        // v2.0.0 新增默认值
        private List<SettingSource> settingSources = Arrays.asList(
            SettingSource.ENV_VARS,
            SettingSource.USER_CONFIG,
            SettingSource.INLINE_OPTIONS
        );
        private Map<String, SubagentDefinition> agents = new HashMap<>();
        private ClaudeTransport customTransport;

        private CliMode cliMode = CliMode.getDefault();
        private Duration ptyReadyTimeout = Duration.ofSeconds(10);
        private String promptPattern;
        private List<String> additionalArgs = new ArrayList<>();

        private int minPoolSize = 2;
        private int maxPoolSize = 10;
        private long connectionTimeout = 5000;
        private long healthCheckInterval = 30000;

        // 基础 Builder 方法
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder cliPath(String cliPath) {
            this.cliPath = cliPath;
            return this;
        }

        public Builder cliEnabled(boolean cliEnabled) {
            this.cliEnabled = cliEnabled;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder enableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = new HashMap<>(environment);
            return this;
        }

        public Builder addEnvironment(String key, String value) {
            this.environment.put(key, value);
            return this;
        }

        public Builder authProvider(AuthenticationProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        // v2.0.0 新增 Builder 方法

        /**
         * 设置配置源（覆盖默认）
         *
         * @param sources 配置源列表
         * @return this
         */
        public Builder settingSources(SettingSource... sources) {
            this.settingSources = Arrays.asList(sources);
            return this;
        }

        /**
         * 添加配置源（追加）
         *
         * @param source 配置源
         * @return this
         */
        public Builder addSettingSource(SettingSource source) {
            if (!this.settingSources.contains(source)) {
                List<SettingSource> newSources = new ArrayList<>(this.settingSources);
                newSources.add(source);
                this.settingSources = newSources;
            }
            return this;
        }

        /**
         * 使用 v1.x 兼容模式（加载所有配置源）
         *
         * @return this
         */
        public Builder useV1CompatibilityMode() {
            this.settingSources = Arrays.asList(SettingSource.values());
            return this;
        }

        /**
         * 添加内联子代理定义
         *
         * @param agent 子代理定义
         * @return this
         */
        public Builder addAgent(SubagentDefinition agent) {
            this.agents.put(agent.getName(), agent);
            return this;
        }

        /**
         * 批量添加子代理
         *
         * @param agents 子代理映射
         * @return this
         */
        public Builder agents(Map<String, SubagentDefinition> agents) {
            this.agents.putAll(agents);
            return this;
        }

        /**
         * 使用自定义传输层
         *
         * @param transport 传输层实现
         * @return this
         */
        public Builder transport(ClaudeTransport transport) {
            this.customTransport = transport;
            return this;
        }

        // CLI 模式 Builder 方法
        public Builder cliMode(CliMode cliMode) {
            this.cliMode = cliMode;
            return this;
        }

        public Builder ptyReadyTimeout(Duration ptyReadyTimeout) {
            this.ptyReadyTimeout = ptyReadyTimeout;
            return this;
        }

        public Builder promptPattern(String promptPattern) {
            this.promptPattern = promptPattern;
            return this;
        }

        public Builder additionalArgs(List<String> additionalArgs) {
            this.additionalArgs = new ArrayList<>(additionalArgs);
            return this;
        }

        public Builder addAdditionalArg(String arg) {
            this.additionalArgs.add(arg);
            return this;
        }

        // 连接池 Builder 方法
        public Builder minPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder healthCheckInterval(long healthCheckInterval) {
            this.healthCheckInterval = healthCheckInterval;
            return this;
        }

        /**
         * 构建 ClaudeAgentOptions 实例
         *
         * @return 配置实例
         */
        public ClaudeAgentOptions build() {
            return new ClaudeAgentOptions(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ClaudeAgentOptions{baseUrl='%s', cliPath='%s', timeout=%s, maxRetries=%d, " +
            "settingSources=%d, agents=%d, customTransport=%s}",
            baseUrl, cliPath, timeout, maxRetries,
            settingSources.size(), agents.size(),
            customTransport != null ? customTransport.getType() : "default"
        );
    }
}
```

---

### 1.3 ClaudeAgentException

**位置**: `com.anthropic.claude.exceptions.ClaudeAgentException`

```java
package com.anthropic.claude.exceptions;

/**
 * Claude Agent SDK 基础异常类
 *
 * <p>所有 SDK 异常的父类</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClaudeAgentException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> context;

    public ClaudeAgentException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.context = Collections.emptyMap();
    }

    public ClaudeAgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.context = Collections.emptyMap();
    }

    public ClaudeAgentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    public ClaudeAgentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    public ClaudeAgentException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = Collections.unmodifiableMap(context);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("ClaudeAgentException[%s]: %s", errorCode, getMessage());
    }
}
```

---

## 2. Session Forking API

### 2.1 SessionManager 增强

**新增方法**:

```java
package com.anthropic.claude.client;

public class SessionManager {

    /**
     * 从现有会话创建分支
     *
     * <p>分支会话继承父会话的消息历史和上下文，
     * 但后续操作相互独立</p>
     *
     * @param parentSessionId 父会话 ID
     * @return 新分支的会话 ID
     * @throws ClaudeAgentException 如果父会话不存在
     */
    public String forkSession(String parentSessionId) {
        Session parent = getSession(parentSessionId);
        if (parent == null) {
            throw new ClaudeAgentException(
                "FORK_FAILED",
                "Parent session not found: " + parentSessionId
            );
        }

        String forkId = generateSessionId();
        Session fork = new Session(forkId, parent);
        sessions.put(forkId, fork);

        logger.info("创建会话分支: {} -> {}", parentSessionId, forkId);
        return forkId;
    }

    /**
     * 获取会话的所有分支
     *
     * @param sessionId 会话 ID
     * @return 分支会话 ID 列表
     */
    public List<String> getSessionForks(String sessionId) {
        return sessions.values().stream()
            .filter(s -> sessionId.equals(s.getParentSessionId()))
            .map(Session::getSessionId)
            .collect(Collectors.toList());
    }

    /**
     * 检查是否为分支会话
     *
     * @param sessionId 会话 ID
     * @return 是否为分支
     */
    public boolean isForkSession(String sessionId) {
        Session session = getSession(sessionId);
        return session != null && session.isFork();
    }

    /**
     * 获取分支会话的父会话 ID
     *
     * @param sessionId 会话 ID
     * @return 父会话 ID，如果不是分支则返回 null
     */
    public String getParentSessionId(String sessionId) {
        Session session = getSession(sessionId);
        return session != null ? session.getParentSessionId() : null;
    }
}
```

### 2.2 Session 类增强

```java
public static class Session {
    private final String sessionId;
    private final String parentSessionId;  // 新增
    private final LocalDateTime createdAt;
    private final LocalDateTime forkedAt;  // 新增
    private final List<Message> inheritedMessages;  // 新增
    private final List<Message> ownMessages;  // 新增
    private volatile long lastActivity;
    private volatile boolean active;
    private final AtomicLong messageCount = new AtomicLong(0);

    /**
     * 创建根会话
     */
    public Session(String sessionId) {
        this.sessionId = sessionId;
        this.parentSessionId = null;
        this.createdAt = LocalDateTime.now();
        this.forkedAt = null;
        this.lastActivity = System.currentTimeMillis();
        this.active = true;
        this.inheritedMessages = Collections.emptyList();
        this.ownMessages = new ArrayList<>();
    }

    /**
     * 创建分支会话（从父会话）
     */
    public Session(String sessionId, Session parent) {
        this.sessionId = sessionId;
        this.parentSessionId = parent.getSessionId();
        this.createdAt = LocalDateTime.now();
        this.forkedAt = LocalDateTime.now();
        this.lastActivity = System.currentTimeMillis();
        this.active = true;

        // 深拷贝父会话的所有消息
        this.inheritedMessages = new ArrayList<>(parent.getAllMessages());
        this.ownMessages = new ArrayList<>();
        this.messageCount.set(inheritedMessages.size());
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getParentSessionId() { return parentSessionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getForkedAt() { return forkedAt; }
    public long getLastActivity() { return lastActivity; }
    public boolean isActive() { return active; }
    public long getMessageCount() { return messageCount.get(); }

    /**
     * 是否为分支会话
     */
    public boolean isFork() {
        return parentSessionId != null;
    }

    /**
     * 获取继承的消息
     */
    public List<Message> getInheritedMessages() {
        return Collections.unmodifiableList(inheritedMessages);
    }

    /**
     * 获取自己的消息
     */
    public List<Message> getOwnMessages() {
        return Collections.unmodifiableList(ownMessages);
    }

    /**
     * 获取所有消息（继承 + 自己）
     */
    public List<Message> getAllMessages() {
        List<Message> all = new ArrayList<>(inheritedMessages);
        all.addAll(ownMessages);
        return Collections.unmodifiableList(all);
    }

    /**
     * 添加新消息
     */
    public void addMessage(Message message) {
        ownMessages.add(message);
        messageCount.incrementAndGet();
        updateLastActivity();
    }

    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void incrementMessageCount() {
        messageCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return String.format(
            "Session{id='%s', parent='%s', created=%s, forked=%s, active=%s, " +
            "inherited=%d, own=%d, total=%d}",
            sessionId, parentSessionId, createdAt, forkedAt, active,
            inheritedMessages.size(), ownMessages.size(), messageCount.get()
        );
    }
}
```

---

## 3. Programmatic Subagents API

### 3.1 SubagentDefinition

**位置**: `com.anthropic.claude.subagents.SubagentDefinition`

```java
package com.anthropic.claude.subagents;

/**
 * 子代理定义
 *
 * <p>用于在代码中定义子代理，而非依赖文件系统</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class SubagentDefinition {
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final List<String> tools;
    private final String model;
    private final Map<String, Object> metadata;
    private final Duration timeout;
    private final int maxRetries;

    private SubagentDefinition(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.description = Objects.requireNonNull(builder.description, "description cannot be null");
        this.systemPrompt = Objects.requireNonNull(builder.systemPrompt, "systemPrompt cannot be null");
        this.tools = Collections.unmodifiableList(builder.tools);
        this.model = builder.model;
        this.metadata = Collections.unmodifiableMap(builder.metadata);
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSystemPrompt() { return systemPrompt; }
    public List<String> getTools() { return tools; }
    public String getModel() { return model; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Duration getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }

    /**
     * 创建 Builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String description;
        private String systemPrompt;
        private List<String> tools = new ArrayList<>();
        private String model = "claude-sonnet-4-5";
        private Map<String, Object> metadata = new HashMap<>();
        private Duration timeout = Duration.ofMinutes(10);
        private int maxRetries = 3;

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder tools(String... tools) {
            this.tools.addAll(Arrays.asList(tools));
            return this;
        }

        public Builder tools(List<String> tools) {
            this.tools.addAll(tools);
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public SubagentDefinition build() {
            if (description == null || systemPrompt == null) {
                throw new IllegalStateException(
                    "description and systemPrompt are required"
                );
            }
            return new SubagentDefinition(this);
        }
    }

    /**
     * 转换为 CLI 参数
     */
    public List<String> toCliArgs() {
        List<String> args = new ArrayList<>();
        args.add("--agent-name");
        args.add(name);
        args.add("--agent-description");
        args.add(description);
        args.add("--agent-prompt");
        args.add(systemPrompt);
        if (!tools.isEmpty()) {
            args.add("--agent-tools");
            args.add(String.join(",", tools));
        }
        args.add("--model");
        args.add(model);
        return args;
    }

    @Override
    public String toString() {
        return String.format(
            "SubagentDefinition{name='%s', model='%s', tools=%d}",
            name, model, tools.size()
        );
    }
}
```

### 3.2 SubagentManager 增强

```java
package com.anthropic.claude.subagents;

public class SubagentManager {
    private final Map<String, SubagentDefinition> inlineAgents = new ConcurrentHashMap<>();

    /**
     * 注册内联子代理定义
     *
     * @param definition 子代理定义
     */
    public void registerInlineAgent(SubagentDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        inlineAgents.put(definition.getName(), definition);
        logger.info("注册内联子代理: {}", definition.getName());
    }

    /**
     * 批量注册子代理
     *
     * @param agents 子代理映射
     */
    public void registerInlineAgents(Map<String, SubagentDefinition> agents) {
        agents.values().forEach(this::registerInlineAgent);
    }

    /**
     * 启动内联定义的子代理
     *
     * @param agentName 子代理名称
     * @param context 上下文数据
     * @return 子代理 ID
     * @throws ClaudeAgentException 如果子代理不存在
     */
    public String startInlineSubagent(String agentName, Map<String, Object> context) {
        SubagentDefinition def = inlineAgents.get(agentName);
        if (def == null) {
            throw new ClaudeAgentException(
                "AGENT_NOT_FOUND",
                "Inline agent not found: " + agentName
            );
        }

        // 将定义转换为 CLI 参数
        List<String> args = def.toCliArgs();

        // 启动子代理进程
        return startSubagentWithArgs(args, context);
    }

    /**
     * 获取所有已注册的内联子代理
     *
     * @return 子代理名称列表
     */
    public Set<String> getRegisteredInlineAgents() {
        return Collections.unmodifiableSet(inlineAgents.keySet());
    }

    /**
     * 检查子代理是否已注册
     *
     * @param agentName 子代理名称
     * @return 是否已注册
     */
    public boolean isInlineAgentRegistered(String agentName) {
        return inlineAgents.containsKey(agentName);
    }

    /**
     * 注销内联子代理
     *
     * @param agentName 子代理名称
     * @return 是否成功注销
     */
    public boolean unregisterInlineAgent(String agentName) {
        SubagentDefinition removed = inlineAgents.remove(agentName);
        if (removed != null) {
            logger.info("注销内联子代理: {}", agentName);
            return true;
        }
        return false;
    }
}
```

---

## 4. Settings Sources API

### 4.1 SettingSource 枚举

**位置**: `com.anthropic.claude.config.SettingSource`

```java
package com.anthropic.claude.config;

/**
 * 配置源枚举
 *
 * <p>定义可用的配置来源及其优先级</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public enum SettingSource {
    /**
     * 默认值（优先级最低）
     */
    DEFAULTS("defaults", 0),

    /**
     * 环境变量
     */
    ENV_VARS("env", 1),

    /**
     * 用户配置文件 (~/.claude/config.properties)
     */
    USER_CONFIG("user_config", 2),

    /**
     * 项目配置文件 (./.claude/config.properties)
     */
    PROJECT_CONFIG("project_config", 3),

    /**
     * CLAUDE.md 文件
     */
    CLAUDE_MD("claude_md", 4),

    /**
     * Slash commands 目录
     */
    SLASH_COMMANDS("slash_commands", 5),

    /**
     * 内联选项（优先级最高）
     */
    INLINE_OPTIONS("inline", 10);

    private final String name;
    private final int priority;

    SettingSource(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return String.format("%s(priority=%d)", name, priority);
    }
}
```

### 4.2 ConfigLoader 重构

```java
package com.anthropic.claude.config;

public class ConfigLoader {

    /**
     * 根据 settingSources 加载配置
     *
     * @param sources 配置源列表
     * @return 合并后的配置
     */
    public ClaudeAgentOptions loadConfiguration(List<SettingSource> sources) {
        Map<String, Object> mergedConfig = new HashMap<>();

        // 按优先级排序（低到高）
        List<SettingSource> sortedSources = sources.stream()
            .sorted(Comparator.comparingInt(SettingSource::getPriority))
            .collect(Collectors.toList());

        // 依次加载并合并
        for (SettingSource source : sortedSources) {
            try {
                Map<String, Object> config = loadFromSource(source);
                mergedConfig.putAll(config);  // 高优先级覆盖低优先级
                logger.debug("已加载配置源: {} (优先级: {})",
                    source.getName(), source.getPriority());
            } catch (Exception e) {
                logger.warn("加载配置源失败: {}, 错误: {}",
                    source.getName(), e.getMessage());
            }
        }

        return buildOptionsFromConfig(mergedConfig);
    }

    /**
     * 从指定源加载配置
     */
    private Map<String, Object> loadFromSource(SettingSource source) {
        switch (source) {
            case DEFAULTS:
                return loadDefaults();
            case ENV_VARS:
                return loadEnvironmentVariables();
            case USER_CONFIG:
                return loadUserConfig();
            case PROJECT_CONFIG:
                return loadProjectConfig();
            case CLAUDE_MD:
                return loadClaudeMd();
            case SLASH_COMMANDS:
                return loadSlashCommands();
            case INLINE_OPTIONS:
                return Collections.emptyMap();  // 由 Builder 直接设置
            default:
                logger.warn("未知配置源: {}", source);
                return Collections.emptyMap();
        }
    }

    private Map<String, Object> loadDefaults() {
        // 实现默认值加载
    }

    private Map<String, Object> loadEnvironmentVariables() {
        // 实现环境变量加载
    }

    private Map<String, Object> loadUserConfig() {
        // 实现用户配置文件加载
    }

    private Map<String, Object> loadProjectConfig() {
        // 实现项目配置文件加载
    }

    private Map<String, Object> loadClaudeMd() {
        // 实现 CLAUDE.md 解析
    }

    private Map<String, Object> loadSlashCommands() {
        // 实现 slash commands 加载
    }
}
```

---

## 5. Custom Transport API

### 5.1 ClaudeTransport 接口

**位置**: `com.anthropic.claude.transport.ClaudeTransport`

```java
package com.anthropic.claude.transport;

/**
 * Claude 传输层接口
 *
 * <p>抽象 Claude CLI 通信，支持自定义实现</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public interface ClaudeTransport extends AutoCloseable {

    /**
     * 发送消息到 Claude
     *
     * @param message 消息内容
     * @throws TransportException 传输错误
     */
    void send(String message) throws TransportException;

    /**
     * 接收 Claude 响应流
     *
     * @return 响应流的 Observable
     * @throws TransportException 传输错误
     */
    Observable<String> receive() throws TransportException;

    /**
     * 检查连接状态
     *
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 关闭传输层并释放资源
     *
     * @throws TransportException 关闭时错误
     */
    @Override
    void close() throws TransportException;

    /**
     * 获取传输层类型
     *
     * @return 传输类型
     */
    TransportType getType();

    /**
     * 获取传输统计信息
     *
     * @return 统计信息
     */
    default TransportStats getStats() {
        return new TransportStats();
    }
}
```

### 5.2 TransportType 枚举

```java
package com.anthropic.claude.transport;

/**
 * 传输层类型
 */
public enum TransportType {
    /**
     * CLI 进程传输（默认）
     */
    CLI_PROCESS,

    /**
     * HTTP API 传输
     */
    HTTP_API,

    /**
     * WebSocket 传输
     */
    WEBSOCKET,

    /**
     * 用户自定义传输
     */
    CUSTOM
}
```

### 5.3 ProcessTransport 实现

**位置**: `com.anthropic.claude.transport.ProcessTransport`

```java
package com.anthropic.claude.transport;

/**
 * 基于 CLI 进程的传输层实现
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

    public ProcessTransport(ProcessManager processManager, ClaudeAgentOptions options) {
        this.processManager = Objects.requireNonNull(processManager);
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public void send(String message) throws TransportException {
        Objects.requireNonNull(message, "message cannot be null");

        try {
            ensureConnected();
            OutputStream stdin = cliProcess.getOutputStream();
            stdin.write(message.getBytes(StandardCharsets.UTF_8));
            stdin.flush();

            stats.incrementMessagesSent();
            stats.addBytesSent(message.length());

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
                    new InputStreamReader(cliProcess.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    stats.incrementMessagesReceived();
                    stats.addBytesReceived(line.length());
                    emitter.onNext(line);
                }
                emitter.onComplete();

            } catch (IOException e) {
                stats.incrementErrors();
                emitter.onError(new TransportException("Failed to receive", e));
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
            cliProcess.destroy();
            try {
                if (!cliProcess.waitFor(5, TimeUnit.SECONDS)) {
                    cliProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransportException("Interrupted while closing", e);
            }
            connected.set(false);
            logger.info("ProcessTransport 已关闭");
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

    /**
     * 确保连接已建立
     */
    private void ensureConnected() throws TransportException {
        if (!isConnected()) {
            startCliProcess();
        }
    }

    /**
     * 启动 CLI 进程
     */
    private void startCliProcess() throws TransportException {
        try {
            List<String> command = buildCommand();
            this.cliProcess = processManager.startProcess(
                command.toArray(new String[0])
            );
            connected.set(true);
            logger.info("CLI 进程已启动: {}", options.getCliPath());

        } catch (Exception e) {
            connected.set(false);
            throw new TransportException("Failed to start CLI process", e);
        }
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        command.add(options.getCliPath());
        command.addAll(options.getAdditionalArgs());
        return command;
    }
}
```

### 5.4 TransportStats 类

```java
package com.anthropic.claude.transport;

/**
 * 传输层统计信息
 */
public class TransportStats {
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    public void incrementMessagesSent() {
        messagesSent.incrementAndGet();
    }

    public void incrementMessagesReceived() {
        messagesReceived.incrementAndGet();
    }

    public void addBytesSent(long bytes) {
        bytesSent.addAndGet(bytes);
    }

    public void addBytesReceived(long bytes) {
        bytesReceived.addAndGet(bytes);
    }

    public void incrementErrors() {
        errors.incrementAndGet();
    }

    public long getMessagesSent() {
        return messagesSent.get();
    }

    public long getMessagesReceived() {
        return messagesReceived.get();
    }

    public long getBytesSent() {
        return bytesSent.get();
    }

    public long getBytesReceived() {
        return bytesReceived.get();
    }

    public long getErrors() {
        return errors.get();
    }

    public long getUptimeMillis() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public String toString() {
        return String.format(
            "TransportStats{sent=%d, received=%d, bytesSent=%d, bytesReceived=%d, " +
            "errors=%d, uptime=%dms}",
            messagesSent.get(), messagesReceived.get(),
            bytesSent.get(), bytesReceived.get(),
            errors.get(), getUptimeMillis()
        );
    }
}
```

---

## 6. Enhanced Hook Fields API

### 6.1 HookResult 增强

**位置**: `com.anthropic.claude.hooks.HookResult`

```java
package com.anthropic.claude.hooks;

/**
 * Hook 执行结果
 *
 * <p>v2.0.0 新增字段：decision, suppressOutput, stopReason</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class HookResult {

    private final HookDecision decision;        // v2.0.0 新增
    private final String reason;                // 重命名自 message
    private final boolean continue_;            // 重命名自 shouldContinue
    private final boolean suppressOutput;       // v2.0.0 新增
    private final String stopReason;            // v2.0.0 新增
    private final Map<String, Object> modifiedData;

    /**
     * Hook 决策类型
     */
    public enum HookDecision {
        /**
         * 批准操作继续
         */
        APPROVE,

        /**
         * 阻止操作
         */
        BLOCK
    }

    private HookResult(Builder builder) {
        this.decision = builder.decision;
        this.reason = builder.reason;
        this.continue_ = builder.continue_;
        this.suppressOutput = builder.suppressOutput;
        this.stopReason = builder.stopReason;
        this.modifiedData = Collections.unmodifiableMap(builder.modifiedData);
    }

    // Getters
    public HookDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public boolean shouldContinue() {
        return continue_;
    }

    public boolean shouldSuppressOutput() {
        return suppressOutput;
    }

    public String getStopReason() {
        return stopReason;
    }

    public Map<String, Object> getModifiedData() {
        return modifiedData;
    }

    /**
     * 兼容性方法
     *
     * @deprecated 使用 {@link #getReason()} 代替
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public String getMessage() {
        return reason;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 快捷方法：批准
     */
    public static HookResult approve() {
        return builder()
            .decision(HookDecision.APPROVE)
            .build();
    }

    /**
     * 快捷方法：批准并提供理由
     */
    public static HookResult approve(String reason) {
        return builder()
            .decision(HookDecision.APPROVE)
            .reason(reason)
            .build();
    }

    /**
     * 快捷方法：阻止
     */
    public static HookResult block(String reason) {
        return builder()
            .decision(HookDecision.BLOCK)
            .reason(reason)
            .continue_(false)
            .build();
    }

    /**
     * 快捷方法：抑制输出
     */
    public static HookResult suppress() {
        return builder()
            .decision(HookDecision.APPROVE)
            .suppressOutput(true)
            .build();
    }

    /**
     * 快捷方法：停止并说明原因
     */
    public static HookResult stop(String stopReason) {
        return builder()
            .decision(HookDecision.APPROVE)
            .stopReason(stopReason)
            .build();
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private HookDecision decision = HookDecision.APPROVE;
        private String reason;
        private boolean continue_ = true;
        private boolean suppressOutput = false;
        private String stopReason;
        private Map<String, Object> modifiedData = new HashMap<>();

        public Builder decision(HookDecision decision) {
            this.decision = Objects.requireNonNull(decision);
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder continue_(boolean continue_) {
            this.continue_ = continue_;
            return this;
        }

        public Builder suppressOutput(boolean suppressOutput) {
            this.suppressOutput = suppressOutput;
            return this;
        }

        public Builder stopReason(String stopReason) {
            this.stopReason = stopReason;
            this.continue_ = false;  // 自动设置
            return this;
        }

        public Builder modifiedData(Map<String, Object> data) {
            this.modifiedData = new HashMap<>(data);
            return this;
        }

        public Builder addModifiedData(String key, Object value) {
            this.modifiedData.put(key, value);
            return this;
        }

        /**
         * 兼容性方法
         *
         * @deprecated 使用 {@link #continue_(boolean)} 代替
         */
        @Deprecated(since = "2.0.0", forRemoval = true)
        public Builder shouldContinue(boolean shouldContinue) {
            return continue_(shouldContinue);
        }

        /**
         * 兼容性方法
         *
         * @deprecated 使用 {@link #reason(String)} 代替
         */
        @Deprecated(since = "2.0.0", forRemoval = true)
        public Builder message(String message) {
            return reason(message);
        }

        public HookResult build() {
            return new HookResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "HookResult{decision=%s, reason='%s', continue=%s, " +
            "suppressOutput=%s, stopReason='%s', modifiedData=%d}",
            decision, reason, continue_, suppressOutput, stopReason,
            modifiedData.size()
        );
    }
}
```

---

## 7. 向后兼容层设计

### 7.1 ClaudeCodeSDK (Deprecated)

**位置**: `com.anthropic.claude.client.ClaudeCodeSDK`

```java
package com.anthropic.claude.client;

/**
 * @deprecated 自 v2.0.0 起已弃用，请使用 {@link ClaudeAgentSDK}
 *             将在 v3.0.0 中移除
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeSDK {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeSDK.class);
    private static final AtomicBoolean WARNING_LOGGED = new AtomicBoolean(false);

    private final ClaudeAgentSDK delegate;

    public ClaudeCodeSDK() {
        logDeprecationWarning();
        this.delegate = new ClaudeAgentSDK();
    }

    public ClaudeCodeSDK(ClaudeCodeOptions options) {
        logDeprecationWarning();
        // 转换 ClaudeCodeOptions -> ClaudeAgentOptions
        this.delegate = new ClaudeAgentSDK(convertOptions(options));
    }

    private void logDeprecationWarning() {
        if (WARNING_LOGGED.compareAndSet(false, true)) {
            logger.warn("╔═══════════════════════════════════════════════════════════════╗");
            logger.warn("║  ⚠️  DEPRECATION WARNING                                      ║");
            logger.warn("║                                                               ║");
            logger.warn("║  ClaudeCodeSDK 已弃用，请使用 ClaudeAgentSDK                   ║");
            logger.warn("║  ClaudeCodeSDK 将在 v3.0.0 中移除                             ║");
            logger.warn("║                                                               ║");
            logger.warn("║  迁移指南:                                                    ║");
            logger.warn("║  https://github.com/.../MIGRATION_GUIDE_V2.md                 ║");
            logger.warn("╚═══════════════════════════════════════════════════════════════╝");
        }
    }

    private ClaudeAgentOptions convertOptions(ClaudeCodeOptions oldOptions) {
        // 转换逻辑
        return ClaudeAgentOptions.builder()
            .apiKey(oldOptions.getApiKey())
            .baseUrl(oldOptions.getBaseUrl())
            .cliPath(oldOptions.getCliPath())
            .timeout(oldOptions.getTimeout())
            .maxRetries(oldOptions.getMaxRetries())
            .environment(oldOptions.getEnvironment())
            .useV1CompatibilityMode()  // 保持 v1.x 行为
            .build();
    }

    // 委托所有方法
    public CompletableFuture<Stream<Message>> query(String prompt) {
        return delegate.query(prompt);
    }

    public CompletableFuture<Stream<Message>> query(QueryRequest request) {
        return delegate.query(request);
    }

    public Observable<Message> queryStream(String prompt) {
        return delegate.queryStream(prompt);
    }

    public Observable<Message> queryStream(QueryRequest request) {
        return delegate.queryStream(request);
    }

    public QueryBuilder queryBuilder(String prompt) {
        return delegate.queryBuilder(prompt);
    }

    public void addHook(String eventType, HookCallback callback) {
        delegate.addHook(eventType, callback);
    }

    public void removeHook(String eventType, HookCallback callback) {
        delegate.removeHook(eventType, callback);
    }

    public SubagentManager getSubagentManager() {
        return delegate.getSubagentManager();
    }

    public ClaudeCodeOptions getConfiguration() {
        // 转换回 ClaudeCodeOptions
        return convertToOldOptions(delegate.getConfiguration());
    }

    public boolean isAuthenticated() {
        return delegate.isAuthenticated();
    }

    public void refreshAuthentication() {
        delegate.refreshAuthentication();
    }

    public boolean isCliAvailable() {
        return delegate.isCliAvailable();
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public boolean healthCheck() {
        return delegate.healthCheck();
    }

    private ClaudeCodeOptions convertToOldOptions(ClaudeAgentOptions newOptions) {
        // 反向转换逻辑
        return ClaudeCodeOptions.builder()
            .apiKey(newOptions.getApiKey())
            .baseUrl(newOptions.getBaseUrl())
            .cliPath(newOptions.getCliPath())
            .timeout(newOptions.getTimeout())
            .maxRetries(newOptions.getMaxRetries())
            .environment(newOptions.getEnvironment())
            .build();
    }
}
```

---

**文档结束**

本 API 设计文档详细定义了 v2.0.0 的所有新增和变更的 API。实施时请严格遵循此设计，确保 API 一致性和向后兼容性。
