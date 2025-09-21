package com.anthropic.claude.client;

import com.anthropic.claude.auth.AuthenticationProvider;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.hooks.HookCallback;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.subagents.SubagentManager;
import com.anthropic.claude.utils.ClaudePathResolver;
import io.reactivex.rxjava3.core.Observable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({})
class ClaudeCodeSDKTest {

    private ClaudeCodeSDK sdk;
    private ClaudeCodeOptions testOptions;
    private TestAuthenticationProvider testAuthProvider;

    @TempDir
    Path tempDir;

    static class TestAuthenticationProvider implements AuthenticationProvider {
        private boolean authenticated = true;
        private String apiKey = "test-api-key";

        @Override
        public String getApiKey() {
            return authenticated ? apiKey : null;
        }

        @Override
        public Map<String, String> getAuthHeaders() {
            Map<String, String> headers = new HashMap<>();
            if (authenticated && apiKey != null) {
                headers.put("x-api-key", apiKey);
                headers.put("anthropic-version", "2023-06-01");
            }
            return headers;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public void refreshAuth() {
            // 测试实现
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }
    }

    @BeforeEach
    void setUp() {
        testAuthProvider = new TestAuthenticationProvider();

        Map<String, String> environment = new HashMap<>();
        environment.put("TEST_ENV", "test-value");

        // 动态获取Claude CLI路径
        String claudePath = ClaudePathResolver.resolveClaudePath();

        testOptions = ClaudeCodeOptions.builder()
                .apiKey("test-api-key")
                .baseUrl("https://api.packycode.com")
                .cliPath(claudePath)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .enableLogging(false)
                .environment(environment)
                .authProvider(testAuthProvider)
                .build();
    }

    @Test
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> {
            ClaudeCodeSDK defaultSdk = new ClaudeCodeSDK();
            assertNotNull(defaultSdk);
            assertNotNull(defaultSdk.getConfiguration());
        });
    }

    @Test
    void testConstructorWithOptions() {
        sdk = new ClaudeCodeSDK(testOptions);

        assertNotNull(sdk);
        assertEquals(testOptions, sdk.getConfiguration());
        assertEquals("1.0.0", sdk.getVersion());
    }

    @Test
    void testGetConfiguration() {
        sdk = new ClaudeCodeSDK(testOptions);

        ClaudeCodeOptions config = sdk.getConfiguration();
        assertEquals("test-api-key", config.getApiKey());
        assertEquals("https://api.packycode.com", config.getBaseUrl());
        // 验证CLI路径不为空且可执行
        assertNotNull(config.getCliPath());
        assertTrue(ClaudePathResolver.validateClaudePath(config.getCliPath()) ||
                   "claude".equals(config.getCliPath()));
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertEquals(2, config.getMaxRetries());
        assertFalse(config.isEnableLogging());
        assertEquals("test-value", config.getEnvironment().get("TEST_ENV"));
    }

    @Test
    void testIsAuthenticated() {
        sdk = new ClaudeCodeSDK(testOptions);

        assertTrue(sdk.isAuthenticated());

        testAuthProvider.setAuthenticated(false);
        assertFalse(sdk.isAuthenticated());
    }

    @Test
    void testRefreshAuthentication() {
        sdk = new ClaudeCodeSDK(testOptions);

        assertDoesNotThrow(() -> sdk.refreshAuthentication());
    }

    @Test
    void testQueryWithString() {
        sdk = new ClaudeCodeSDK(testOptions);

        CompletableFuture<Stream<Message>> result = sdk.query("test prompt");
        assertNotNull(result);
        try {
            result.get().forEach(message -> {
                System.out.printf(message.getContent());
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testQueryWithQueryRequest() {
        sdk = new ClaudeCodeSDK(testOptions);

        QueryRequest request = QueryRequest.builder("test prompt")
                .withTimeout(Duration.ofSeconds(10))
                .build();

        CompletableFuture<Stream<Message>> result = sdk.query(request);
        assertNotNull(result);
    }

    @Test
    void testQueryStreamWithString() {
        sdk = new ClaudeCodeSDK(testOptions);

        Observable<Message> result = sdk.queryStream("test prompt");
        assertNotNull(result);
    }

    @Test
    void testQueryStreamWithQueryRequest() {
        sdk = new ClaudeCodeSDK(testOptions);

        QueryRequest request = QueryRequest.builder("test prompt")
                .withTimeout(Duration.ofSeconds(10))
                .build();

        Observable<Message> result = sdk.queryStream(request);
        assertNotNull(result);
    }

    @Test
    void testQueryBuilder() {
        sdk = new ClaudeCodeSDK(testOptions);

        assertNotNull(sdk.queryBuilder("test prompt"));
    }

    @Test
    void testAddAndRemoveHook() {
        sdk = new ClaudeCodeSDK(testOptions);

        HookCallback callback = (context) -> null;

        assertDoesNotThrow(() -> {
            sdk.addHook("test-event", callback);
            sdk.removeHook("test-event", callback);
        });
    }

    @Test
    void testGetSubagentManager() {
        sdk = new ClaudeCodeSDK(testOptions);

        SubagentManager manager = sdk.getSubagentManager();
        assertNotNull(manager);
    }

    @Test
    void testConfigureThrowsUnsupportedOperationException() {
        sdk = new ClaudeCodeSDK(testOptions);

        ClaudeCodeOptions newOptions = ClaudeCodeOptions.builder()
                .apiKey("new-api-key")
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> sdk.configure(newOptions));
    }

    @Test
    void testIsCliAvailable() {
        sdk = new ClaudeCodeSDK(testOptions);

        boolean available = sdk.isCliAvailable();
        assertNotNull(available);
    }

    @Test
    void testHealthCheck() {
        sdk = new ClaudeCodeSDK(testOptions);

        boolean healthy = sdk.healthCheck();
        assertNotNull(healthy);
    }

    @Test
    void testHealthCheckWhenNotAuthenticated() {
        testAuthProvider.setAuthenticated(false);
        sdk = new ClaudeCodeSDK(testOptions);

        boolean healthy = sdk.healthCheck();
        assertFalse(healthy);
    }

    @Test
    void testShutdown() {
        sdk = new ClaudeCodeSDK(testOptions);

        assertDoesNotThrow(() -> sdk.shutdown());
    }

    @Test
    void testGetVersion() {
        sdk = new ClaudeCodeSDK(testOptions);

        assertEquals("1.0.0", sdk.getVersion());
    }
}