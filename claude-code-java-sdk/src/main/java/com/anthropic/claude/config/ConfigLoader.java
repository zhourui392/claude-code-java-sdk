package com.anthropic.claude.config;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String DEFAULT_CONFIG_FILE = "claude-code.properties";
    private static final String CLAUDE_DIR = ".claude";
    private static final String USER_CONFIG_FILE = "config.properties";

    private final Map<String, String> configuration;
    private final Map<String, String> environmentOverrides;

    public ConfigLoader() {
        this.configuration = new HashMap<>();
        this.environmentOverrides = new HashMap<>();
        loadConfiguration();
    }

    private void loadConfiguration() {
        loadDefaultConfiguration();
        loadUserConfiguration();
        loadEnvironmentVariables();
        applyEnvironmentOverrides();
    }

    private void loadDefaultConfiguration() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                props.forEach((key, value) -> configuration.put(key.toString(), value.toString()));
                logger.debug("加载默认配置: {}", DEFAULT_CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.warn("无法加载默认配置文件: {}", DEFAULT_CONFIG_FILE, e);
        }
    }

    private void loadUserConfiguration() {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path claudeDir = userHome.resolve(CLAUDE_DIR);
        Path configFile = claudeDir.resolve(USER_CONFIG_FILE);

        if (Files.exists(configFile)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(configFile));
                props.forEach((key, value) -> configuration.put(key.toString(), value.toString()));
                logger.debug("加载用户配置: {}", configFile);
            } catch (IOException e) {
                logger.warn("无法加载用户配置文件: {}", configFile, e);
            }
        } else {
            logger.debug("用户配置文件不存在: {}", configFile);
        }
    }

    private void loadEnvironmentVariables() {
        Map<String, String> env = System.getenv();
        env.forEach((key, value) -> {
            if (key.startsWith("CLAUDE_CODE_")) {
                String configKey = key.toLowerCase().replace("claude_code_", "").replace("_", ".");
                environmentOverrides.put(configKey, value);
            }
        });

        String apiKey = env.get("ANTHROPIC_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            environmentOverrides.put("api.key", apiKey);
        }

        String authToken = env.get("ANTHROPIC_AUTH_TOKEN");
        if (authToken != null && !authToken.isEmpty()) {
            environmentOverrides.put("api.key", authToken);
        }

        String baseUrl = env.get("ANTHROPIC_BASE_URL");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            environmentOverrides.put("base.url", baseUrl);
        }

        logger.debug("加载环境变量配置: {}", environmentOverrides.size());
    }

    private void applyEnvironmentOverrides() {
        environmentOverrides.forEach((key, value) -> {
            configuration.put(key, value);
            logger.debug("应用环境变量覆盖: {} = ***", key);
        });
    }

    public ClaudeCodeOptions createOptions() {
        return ClaudeCodeOptions.builder()
                .apiKey(getApiKey())
                .baseUrl(getBaseUrl())
                .cliPath(getCliPath())
                .cliEnabled(isCliEnabled())
                .timeout(getTimeout())
                .maxRetries(getMaxRetries())
                .enableLogging(isLoggingEnabled())
                .environment(getEnvironmentVariables())
                .build();
    }

    public String getApiKey() {
        return getProperty("api.key", null);
    }

    public String getBaseUrl() {
        return getProperty("base.url", "https://api.anthropic.com");
    }

    public String getCliPath() {
        String cliPath = getProperty("cli.path", "claude-code");

        // 如果显式设置为空字符串，则返回空字符串（用于验证）
        if (hasProperty("cli.path") && (cliPath == null || cliPath.trim().isEmpty())) {
            return cliPath;
        }

        if (isWindowsSystem()) {
            if (!cliPath.endsWith(".exe") && !cliPath.contains("/") && !cliPath.contains("\\")) {
                cliPath += ".exe";
            }
        }

        return cliPath;
    }

    public Duration getTimeout() {
        String timeoutStr = getProperty("timeout.seconds", "600");
        try {
            long seconds = Long.parseLong(timeoutStr);
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            logger.warn("无效的超时配置: {}, 使用默认值", timeoutStr);
            return Duration.ofMinutes(10);
        }
    }

    public int getMaxRetries() {
        String retriesStr = getProperty("max.retries", "3");
        try {
            return Integer.parseInt(retriesStr);
        } catch (NumberFormatException e) {
            logger.warn("无效的重试次数配置: {}, 使用默认值", retriesStr);
            return 3;
        }
    }

    public boolean isLoggingEnabled() {
        String loggingStr = getProperty("logging.enabled", "true");
        return Boolean.parseBoolean(loggingStr);
    }

    public boolean isCliEnabled() {
        String enabled = getProperty("cli.enabled", "false");
        return Boolean.parseBoolean(enabled);
    }

    public Map<String, String> getEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();

        configuration.forEach((key, value) -> {
            if (key.startsWith("env.")) {
                String envKey = key.substring(4).toUpperCase().replace(".", "_");
                envVars.put(envKey, value);
            }
        });

        if (getApiKey() != null) {
            envVars.put("ANTHROPIC_API_KEY", getApiKey());
        }

        if (getBaseUrl() != null) {
            envVars.put("ANTHROPIC_BASE_URL", getBaseUrl());
        }

        return envVars;
    }

    public String getProperty(String key) {
        return configuration.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        configuration.put(key, value);
    }

    public boolean hasProperty(String key) {
        return configuration.containsKey(key);
    }

    public void validateConfiguration() throws ClaudeCodeException {
//        if (getApiKey() == null || getApiKey().trim().isEmpty()) {
//            throw new ClaudeCodeException("CONFIG_VALIDATION_ERROR",
//                    "API密钥未配置。请设置 ANTHROPIC_API_KEY 环境变量或在配置文件中设置 api.key");
//        }

        if (getCliPath() == null || getCliPath().trim().isEmpty()) {
            throw new ClaudeCodeException("CONFIG_VALIDATION_ERROR", "CLI路径未配置");
        }

        if (getTimeout().isNegative() || getTimeout().isZero()) {
            throw new ClaudeCodeException("CONFIG_VALIDATION_ERROR", "超时时间必须大于0");
        }

        if (getMaxRetries() < 0) {
            throw new ClaudeCodeException("CONFIG_VALIDATION_ERROR", "重试次数不能为负数");
        }
    }

    private boolean isWindowsSystem() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    public Map<String, String> getAllProperties() {
        return new HashMap<>(configuration);
    }
}
