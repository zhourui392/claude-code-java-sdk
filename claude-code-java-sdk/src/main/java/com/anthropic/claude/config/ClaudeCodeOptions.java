package com.anthropic.claude.config;

import com.anthropic.claude.auth.AuthenticationProvider;
import com.anthropic.claude.utils.ClaudePathResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaudeCodeOptions {
    private final String apiKey;
    private final String baseUrl;
    private final String cliPath;
    private final boolean cliEnabled;
    private final Duration timeout;
    private final int maxRetries;
    private final boolean enableLogging;
    private final Map<String, String> environment;
    private final AuthenticationProvider authProvider;

    // CLI模式配置
    private final CliMode cliMode;
    private final Duration ptyReadyTimeout;
    private final String promptPattern;
    private final List<String> additionalArgs;

    // 连接池配置
    private final int minPoolSize;
    private final int maxPoolSize;
    private final long connectionTimeout;
    private final long healthCheckInterval;

    private ClaudeCodeOptions(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.cliPath = builder.cliPath;
        this.cliEnabled = builder.cliEnabled;
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.enableLogging = builder.enableLogging;
        this.environment = builder.environment;
        this.authProvider = builder.authProvider;
        this.cliMode = builder.cliMode;
        this.ptyReadyTimeout = builder.ptyReadyTimeout;
        this.promptPattern = builder.promptPattern;
        this.additionalArgs = builder.additionalArgs;
        this.minPoolSize = builder.minPoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.healthCheckInterval = builder.healthCheckInterval;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCliPath() {
        return cliPath;
    }

    public boolean isCliEnabled() {
        return cliEnabled;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public Map<String, String> getEnvironment() {
        return new HashMap<>(environment);
    }

    public AuthenticationProvider getAuthProvider() {
        return authProvider;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public CliMode getCliMode() {
        return cliMode;
    }

    public Duration getPtyReadyTimeout() {
        return ptyReadyTimeout;
    }

    public String getPromptPattern() {
        return promptPattern;
    }

    public List<String> getAdditionalArgs() {
        return additionalArgs != null ? new ArrayList<>(additionalArgs) : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com";
        private String cliPath;
        private boolean cliEnabled = false;
        private Duration timeout = Duration.ofMinutes(10);
        private int maxRetries = 3;
        private boolean enableLogging = true;
        private Map<String, String> environment = new HashMap<>();
        private AuthenticationProvider authProvider;

        // CLI模式配置默认值
        private CliMode cliMode = CliMode.getDefault();
        private Duration ptyReadyTimeout = Duration.ofSeconds(10);
        private String promptPattern;
        private List<String> additionalArgs = new ArrayList<>();

        // 连接池配置默认值
        private int minPoolSize = 2;
        private int maxPoolSize = 10;
        private long connectionTimeout = 5000; // 5秒
        private long healthCheckInterval = 30000; // 30秒

        public Builder() {
            // 动态获取Claude CLI路径作为默认值
            this.cliPath = ClaudePathResolver.resolveClaudePath();
        }

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

        public ClaudeCodeOptions build() {
            return new ClaudeCodeOptions(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ClaudeCodeOptions{baseUrl='%s', cliPath='%s', timeout=%s, maxRetries=%d, enableLogging=%s}",
                baseUrl, cliPath, timeout, maxRetries, enableLogging);
    }
}
