package com.anthropic.claude.config;

import com.anthropic.claude.auth.AuthenticationProvider;
import com.anthropic.claude.subagents.SubagentDefinition;
import com.anthropic.claude.transport.ClaudeTransport;
import com.anthropic.claude.utils.ClaudePathResolver;

import java.time.Duration;
import java.util.*;

/**
 * Claude Agent SDK 配置选项
 *
 * <p>使用 Builder 模式创建配置实例。</p>
 *
 * <h3>基础使用</h3>
 * <pre>{@code
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .apiKey(System.getenv("ANTHROPIC_API_KEY"))
 *     .timeout(Duration.ofMinutes(10))
 *     .build();
 * }</pre>
 *
 * <h3>v2.0.0 新特性</h3>
 * <pre>{@code
 * // 自定义配置源
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .settingSources(SettingSource.ENV_VARS, SettingSource.USER_CONFIG)
 *     .build();
 *
 * // 内联子代理
 * SubagentDefinition agent = SubagentDefinition.builder("code-reviewer")
 *     .description("Reviews code")
 *     .systemPrompt("You are a code reviewer")
 *     .tools("Read", "Grep")
 *     .build();
 *
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .addAgent(agent)
 *     .build();
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClaudeAgentOptions {

    // 基础配置（继承自 v1.0.0）
    private final String apiKey;
    private final String baseUrl;
    private final String cliPath;
    private final boolean cliEnabled;
    private final Duration timeout;
    private final int maxRetries;
    private final boolean enableLogging;
    private final Map<String, String> environment;
    private final AuthenticationProvider authProvider;

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

    // v2.0.0 新增配置
    private final List<SettingSource> settingSources;
    private final Map<String, SubagentDefinition> agents;
    private final ClaudeTransport customTransport;
    private final boolean suppressDeprecationWarnings;

    private ClaudeAgentOptions(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.cliPath = builder.cliPath;
        this.cliEnabled = builder.cliEnabled;
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.enableLogging = builder.enableLogging;
        this.environment = Collections.unmodifiableMap(new HashMap<>(builder.environment));
        this.authProvider = builder.authProvider;
        this.cliMode = builder.cliMode;
        this.ptyReadyTimeout = builder.ptyReadyTimeout;
        this.promptPattern = builder.promptPattern;
        this.additionalArgs = Collections.unmodifiableList(new ArrayList<>(builder.additionalArgs));
        this.minPoolSize = builder.minPoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.healthCheckInterval = builder.healthCheckInterval;

        // v2.0.0 新字段
        this.settingSources = Collections.unmodifiableList(new ArrayList<>(builder.settingSources));
        this.agents = Collections.unmodifiableMap(new HashMap<>(builder.agents));
        this.customTransport = builder.customTransport;
        this.suppressDeprecationWarnings = builder.suppressDeprecationWarnings;
    }

    // ====== 基础配置 Getters ======

    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getCliPath() { return cliPath; }
    public boolean isCliEnabled() { return cliEnabled; }
    public Duration getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isEnableLogging() { return enableLogging; }
    public Map<String, String> getEnvironment() { return environment; }
    public AuthenticationProvider getAuthProvider() { return authProvider; }

    // ====== CLI 模式 Getters ======

    public CliMode getCliMode() { return cliMode; }
    public Duration getPtyReadyTimeout() { return ptyReadyTimeout; }
    public String getPromptPattern() { return promptPattern; }
    public List<String> getAdditionalArgs() { return additionalArgs; }

    // ====== 连接池 Getters ======

    public int getMinPoolSize() { return minPoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getHealthCheckInterval() { return healthCheckInterval; }

    // ====== v2.0.0 新增 Getters ======

    public List<SettingSource> getSettingSources() { return settingSources; }
    public Map<String, SubagentDefinition> getAgents() { return agents; }
    public ClaudeTransport getCustomTransport() { return customTransport; }
    public boolean isSuppressDeprecationWarnings() { return suppressDeprecationWarnings; }

    /**
     * 创建 Builder 实例
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ClaudeAgentOptions Builder
     */
    public static class Builder {
        // 基础配置默认值
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com";
        private String cliPath = ClaudePathResolver.resolveClaudePath();
        private boolean cliEnabled = false;
        private Duration timeout = Duration.ofMinutes(10);
        private int maxRetries = 3;
        private boolean enableLogging = true;
        private Map<String, String> environment = new HashMap<>();
        private AuthenticationProvider authProvider;

        // CLI 模式默认值
        private CliMode cliMode = CliMode.getDefault();
        private Duration ptyReadyTimeout = Duration.ofSeconds(10);
        private String promptPattern;
        private List<String> additionalArgs = new ArrayList<>();

        // 连接池默认值
        private int minPoolSize = 2;
        private int maxPoolSize = 10;
        private long connectionTimeout = 5000;
        private long healthCheckInterval = 30000;

        // v2.0.0 新增默认值
        private List<SettingSource> settingSources = Arrays.asList(
            SettingSource.ENV_VARS,
            SettingSource.USER_CONFIG,
            SettingSource.INLINE_OPTIONS
        );
        private Map<String, SubagentDefinition> agents = new HashMap<>();
        private ClaudeTransport customTransport;
        private boolean suppressDeprecationWarnings = false;

        // ====== 基础配置 Builder 方法 ======

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

        // ====== CLI 模式 Builder 方法 ======

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

        // ====== 连接池 Builder 方法 ======

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

        // ====== v2.0.0 新增 Builder 方法 ======

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

        /**
         * 设置是否抑制弃用警告
         *
         * @param suppress 是否抑制
         * @return this
         */
        public Builder suppressDeprecationWarnings(boolean suppress) {
            this.suppressDeprecationWarnings = suppress;
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
