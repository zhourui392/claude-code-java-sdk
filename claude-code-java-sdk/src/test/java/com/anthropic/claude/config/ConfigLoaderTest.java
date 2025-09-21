package com.anthropic.claude.config;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    private String originalApiKey;
    private String originalCliPath;
    private String originalTimeout;
    private String originalRetries;
    private String originalLogging;

    @BeforeEach
    void setUp() {
        originalApiKey = System.getenv("ANTHROPIC_API_KEY");
        originalCliPath = System.getenv("CLAUDE_CODE_CLI_PATH");
        originalTimeout = System.getenv("CLAUDE_CODE_TIMEOUT_SECONDS");
        originalRetries = System.getenv("CLAUDE_CODE_MAX_RETRIES");
        originalLogging = System.getenv("CLAUDE_CODE_LOGGING_ENABLED");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testDefaultConfiguration() {
        ConfigLoader loader = new ConfigLoader();

        String cliPath = loader.getCliPath();
        assertNotNull(cliPath);
        assertTrue(cliPath.length() > 0);

        Duration timeout = loader.getTimeout();
        assertNotNull(timeout);
        assertTrue(timeout.getSeconds() > 0);

        int maxRetries = loader.getMaxRetries();
        assertTrue(maxRetries >= 0);

        boolean loggingEnabled = loader.isLoggingEnabled();
        assertNotNull(loggingEnabled);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testWindowsCliPathHandling() {
        ConfigLoader loader = new ConfigLoader();

        loader.setProperty("cli.path", "claude-code");
        String cliPath = loader.getCliPath();

        assertTrue(cliPath.endsWith(".exe"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testUnixCliPathHandling() {
        ConfigLoader loader = new ConfigLoader();

        loader.setProperty("cli.path", "claude-code");
        String cliPath = loader.getCliPath();

        assertFalse(cliPath.endsWith(".exe"));
    }

    @Test
    void testTimeoutParsing() {
        ConfigLoader loader = new ConfigLoader();

        loader.setProperty("timeout.seconds", "300");
        assertEquals(Duration.ofSeconds(300), loader.getTimeout());

        loader.setProperty("timeout.seconds", "invalid");
        assertEquals(Duration.ofMinutes(10), loader.getTimeout());
    }

    @Test
    void testMaxRetriesParsing() {
        ConfigLoader loader = new ConfigLoader();

        loader.setProperty("max.retries", "5");
        assertEquals(5, loader.getMaxRetries());

        loader.setProperty("max.retries", "invalid");
        assertEquals(3, loader.getMaxRetries());
    }

    @Test
    void testLoggingEnabledParsing() {
        ConfigLoader loader = new ConfigLoader();

        loader.setProperty("logging.enabled", "false");
        assertFalse(loader.isLoggingEnabled());

        loader.setProperty("logging.enabled", "true");
        assertTrue(loader.isLoggingEnabled());

        loader.setProperty("logging.enabled", "invalid");
        assertFalse(loader.isLoggingEnabled());
    }

    @Test
    void testPropertyAccessors() {
        ConfigLoader loader = new ConfigLoader();

        assertFalse(loader.hasProperty("non.existent.property"));
        assertNull(loader.getProperty("non.existent.property"));
        assertEquals("default", loader.getProperty("non.existent.property", "default"));

        loader.setProperty("test.property", "test-value");
        assertTrue(loader.hasProperty("test.property"));
        assertEquals("test-value", loader.getProperty("test.property"));
        assertEquals("test-value", loader.getProperty("test.property", "default"));
    }

    @Test
    void testEnvironmentVariableProcessing() {
        ConfigLoader loader = new ConfigLoader();

        Map<String, String> envVars = loader.getEnvironmentVariables();
        assertNotNull(envVars);

        loader.setProperty("env.test.var", "test-value");
        envVars = loader.getEnvironmentVariables();
        assertEquals("test-value", envVars.get("TEST_VAR"));
    }

    @Test
    void testCreateOptions() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("api.key", "test-api-key");
        loader.setProperty("base.url", "https://api.anthropic.com");

        ClaudeCodeOptions options = loader.createOptions();
        assertNotNull(options);
        assertEquals("test-api-key", options.getApiKey());
        assertEquals("https://api.anthropic.com", options.getBaseUrl());
    }

    @Test
    void testBaseUrlConfiguration() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("api.key", "test-api-key");
        loader.setProperty("base.url", "https://api.packycode.com");

        ClaudeCodeOptions options = loader.createOptions();
        assertNotNull(options);
        assertEquals("https://api.packycode.com", options.getBaseUrl());

        Map<String, String> envVars = loader.getEnvironmentVariables();
        assertEquals("https://api.packycode.com", envVars.get("ANTHROPIC_BASE_URL"));
    }

    @Test
    void testValidateConfigurationSuccess() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("api.key", "test-api-key");
        loader.setProperty("cli.path", "claude-code");
        loader.setProperty("timeout.seconds", "600");
        loader.setProperty("max.retries", "3");

        assertDoesNotThrow(() -> loader.validateConfiguration());
    }

    @Test
    void testValidateConfigurationFailsWithEmptyCliPath() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("api.key", "test-api-key");
        loader.setProperty("cli.path", "");

        ClaudeCodeException exception = assertThrows(ClaudeCodeException.class,
                () -> loader.validateConfiguration());
        assertTrue(exception.getMessage().contains("CLI路径未配置"));
    }

    @Test
    void testValidateConfigurationFailsWithZeroTimeout() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("api.key", "test-api-key");
        loader.setProperty("timeout.seconds", "0");

        ClaudeCodeException exception = assertThrows(ClaudeCodeException.class,
                () -> loader.validateConfiguration());
        assertTrue(exception.getMessage().contains("超时时间必须大于0"));
    }

    @Test
    void testValidateConfigurationFailsWithNegativeRetries() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("api.key", "test-api-key");
        loader.setProperty("max.retries", "-1");

        ClaudeCodeException exception = assertThrows(ClaudeCodeException.class,
                () -> loader.validateConfiguration());
        assertTrue(exception.getMessage().contains("重试次数不能为负数"));
    }

    @Test
    void testGetAllProperties() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("test.key1", "value1");
        loader.setProperty("test.key2", "value2");

        Map<String, String> allProps = loader.getAllProperties();
        assertEquals("value1", allProps.get("test.key1"));
        assertEquals("value2", allProps.get("test.key2"));

        allProps.put("test.key3", "value3");
        assertNull(loader.getProperty("test.key3"));
    }
}