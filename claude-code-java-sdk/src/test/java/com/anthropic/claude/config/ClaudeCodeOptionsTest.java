package com.anthropic.claude.config;

import com.anthropic.claude.auth.AuthenticationProvider;
import com.anthropic.claude.utils.ClaudePathResolver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeCodeOptionsTest {

    @Test
    void testBuilderWithAllOptions() {
        TestAuthenticationProvider authProvider = new TestAuthenticationProvider();
        Map<String, String> environment = new HashMap<>();
        environment.put("TEST_KEY", "test-value");

        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-api-key")
                .baseUrl("https://api.packycode.com")
                .cliPath("test-cli-path")
                .timeout(Duration.ofMinutes(5))
                .maxRetries(5)
                .enableLogging(false)
                .environment(environment)
                .authProvider(authProvider)
                .build();

        assertEquals("test-api-key", options.getApiKey());
        assertEquals("https://api.packycode.com", options.getBaseUrl());
        assertEquals("test-cli-path", options.getCliPath());
        assertEquals(Duration.ofMinutes(5), options.getTimeout());
        assertEquals(5, options.getMaxRetries());
        assertFalse(options.isEnableLogging());
        assertEquals("test-value", options.getEnvironment().get("TEST_KEY"));
        assertEquals(authProvider, options.getAuthProvider());
    }

    @Test
    void testBuilderWithDefaults() {
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-api-key")
                .build();

        assertEquals("test-api-key", options.getApiKey());
        assertEquals("https://api.anthropic.com", options.getBaseUrl());

        // 验证CLI路径是通过ClaudePathResolver动态解析的
        String expectedPath = ClaudePathResolver.resolveClaudePath();
        assertEquals(expectedPath, options.getCliPath());
        assertNotNull(options.getCliPath());
        assertFalse(options.getCliPath().trim().isEmpty());

        assertEquals(Duration.ofMinutes(10), options.getTimeout());
        assertEquals(3, options.getMaxRetries());
        assertTrue(options.isEnableLogging());
        assertTrue(options.getEnvironment().isEmpty());
        assertNull(options.getAuthProvider());
    }

    @Test
    void testAddEnvironment() {
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-api-key")
                .addEnvironment("KEY1", "value1")
                .addEnvironment("KEY2", "value2")
                .build();

        Map<String, String> env = options.getEnvironment();
        assertEquals("value1", env.get("KEY1"));
        assertEquals("value2", env.get("KEY2"));
    }

    @Test
    void testEnvironmentMapImmutable() {
        Map<String, String> originalEnv = new HashMap<>();
        originalEnv.put("ORIGINAL", "value");

        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-api-key")
                .environment(originalEnv)
                .build();

        originalEnv.put("NEW_KEY", "new-value");

        Map<String, String> optionsEnv = options.getEnvironment();
        assertFalse(optionsEnv.containsKey("NEW_KEY"));

        Map<String, String> env2 = options.getEnvironment();
        assertNotSame(optionsEnv, env2);
    }

    @Test
    void testToString() {
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-api-key")
                .cliPath("custom-cli")
                .timeout(Duration.ofMinutes(5))
                .maxRetries(2)
                .enableLogging(true)
                .build();

        String toString = options.toString();
        assertTrue(toString.contains("custom-cli"));
        assertTrue(toString.contains("PT5M"));
        assertTrue(toString.contains("2"));
        assertTrue(toString.contains("true"));
    }

    static class TestAuthenticationProvider implements AuthenticationProvider {
        @Override
        public String getApiKey() {
            return "test-api-key";
        }

        @Override
        public Map<String, String> getAuthHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("x-api-key", "test-api-key");
            headers.put("anthropic-version", "2023-06-01");
            return headers;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void refreshAuth() {
        }
    }
}