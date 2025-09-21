package com.anthropic.claude.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * SDK配置类
 *
 * @author Claude Code Team
 * @since 1.0.0
 */
public class Configuration {

    private String apiKey;
    private String apiUrl = "https://api.anthropic.com/v1";
    private String model = "claude-3-opus";
    private Duration timeout = Duration.ofSeconds(30);
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private boolean streamingEnabled = true;
    private Map<String, String> headers = new HashMap<>();
    private ProxyConfig proxyConfig;
    private LogLevel logLevel = LogLevel.INFO;

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * 代理配置
     */
    public static class ProxyConfig {
        private String host;
        private int port;
        private String username;
        private String password;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * 从环境变量加载配置
     *
     * @return 配置对象
     */
    public static Configuration fromEnvironment() {
        Configuration config = new Configuration();

        Optional.ofNullable(System.getenv("CLAUDE_API_KEY"))
            .ifPresent(config::setApiKey);

        Optional.ofNullable(System.getenv("CLAUDE_API_URL"))
            .ifPresent(config::setApiUrl);

        Optional.ofNullable(System.getenv("CLAUDE_MODEL"))
            .ifPresent(config::setModel);

        Optional.ofNullable(System.getenv("CLAUDE_TIMEOUT"))
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .ifPresent(config::setTimeout);

        Optional.ofNullable(System.getenv("CLAUDE_MAX_RETRIES"))
            .map(Integer::parseInt)
            .ifPresent(config::setMaxRetries);

        return config;
    }

    /**
     * 验证配置是否有效
     *
     * @throws IllegalStateException 如果配置无效
     */
    public void validate() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("API key is required");
        }
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new IllegalStateException("API URL is required");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException("Timeout must be positive");
        }
        if (maxRetries < 0) {
            throw new IllegalStateException("Max retries must be non-negative");
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configuration that = (Configuration) o;
        return Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(apiUrl, that.apiUrl) &&
               Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, apiUrl, model);
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private final Configuration config = new Configuration();

        public Builder apiKey(String apiKey) {
            config.apiKey = apiKey;
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            config.apiUrl = apiUrl;
            return this;
        }

        public Builder model(String model) {
            config.model = model;
            return this;
        }

        public Builder timeout(Duration timeout) {
            config.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            config.maxRetries = maxRetries;
            return this;
        }

        public Builder retryDelay(Duration retryDelay) {
            config.retryDelay = retryDelay;
            return this;
        }

        public Builder streamingEnabled(boolean enabled) {
            config.streamingEnabled = enabled;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            config.headers = headers;
            return this;
        }

        public Builder proxyConfig(ProxyConfig proxyConfig) {
            config.proxyConfig = proxyConfig;
            return this;
        }

        public Builder logLevel(LogLevel logLevel) {
            config.logLevel = logLevel;
            return this;
        }

        public Configuration build() {
            config.validate();
            return config;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}