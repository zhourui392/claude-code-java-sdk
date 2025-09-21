package com.anthropic.claude.auth;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证提供者工厂测试
 */
class AuthenticationProviderFactoryTest {

    @Test
    void testProviderTypeFromCode() {
        assertEquals(AuthenticationProviderFactory.ProviderType.DEFAULT,
                AuthenticationProviderFactory.ProviderType.fromCode("default"));
        assertEquals(AuthenticationProviderFactory.ProviderType.BEDROCK,
                AuthenticationProviderFactory.ProviderType.fromCode("bedrock"));
        assertEquals(AuthenticationProviderFactory.ProviderType.VERTEX_AI,
                AuthenticationProviderFactory.ProviderType.fromCode("vertex"));

        assertThrows(ClaudeCodeException.class, () ->
                AuthenticationProviderFactory.ProviderType.fromCode("unknown"));
    }

    @Test
    void testCreateDefaultProviderWithConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("apiKey", "test-api-key");

        AuthenticationProvider provider = AuthenticationProviderFactory.createDefaultProvider(config);
        assertNotNull(provider);
        assertInstanceOf(DefaultAuthenticationProvider.class, provider);
    }

    @Test
    void testCreateBedrockProviderWithConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("accessKey", "AKIAIOSFODNN7EXAMPLE");
        config.put("secretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        config.put("region", "us-east-1");

        AuthenticationProvider provider = AuthenticationProviderFactory.createBedrockProvider(config);
        assertNotNull(provider);
        assertInstanceOf(BedrockAuthenticationProvider.class, provider);

        BedrockAuthenticationProvider bedrockProvider = (BedrockAuthenticationProvider) provider;
        assertEquals("us-east-1", bedrockProvider.getRegion());
    }

    @Test
    void testCreateVertexAIProviderWithConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("projectId", "test-project-123");
        config.put("location", "us-central1");

        AuthenticationProvider provider = AuthenticationProviderFactory.createVertexAIProvider(config);
        assertNotNull(provider);
        assertInstanceOf(VertexAIAuthenticationProvider.class, provider);

        VertexAIAuthenticationProvider vertexProvider = (VertexAIAuthenticationProvider) provider;
        assertEquals("test-project-123", vertexProvider.getProjectId());
        assertEquals("us-central1", vertexProvider.getLocation());
    }

    @Test
    void testBuilderPattern() {
        AuthenticationProvider provider = AuthenticationProviderFactory
                .builder(AuthenticationProviderFactory.ProviderType.DEFAULT)
                .apiKey("test-api-key")
                .build();

        assertNotNull(provider);
        assertInstanceOf(DefaultAuthenticationProvider.class, provider);
    }

    @Test
    void testBuilderPatternForBedrock() {
        AuthenticationProvider provider = AuthenticationProviderFactory
                .builder(AuthenticationProviderFactory.ProviderType.BEDROCK)
                .awsCredentials("AKIAIOSFODNN7EXAMPLE",
                              "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                              "us-west-2")
                .build();

        assertNotNull(provider);
        assertInstanceOf(BedrockAuthenticationProvider.class, provider);
    }

    @Test
    void testBuilderPatternForVertexAI() {
        AuthenticationProvider provider = AuthenticationProviderFactory
                .builder(AuthenticationProviderFactory.ProviderType.VERTEX_AI)
                .googleCloudConfig("my-project", "us-central1")
                .build();

        assertNotNull(provider);
        assertInstanceOf(VertexAIAuthenticationProvider.class, provider);
    }

    @Test
    void testGetAvailableProviders() {
        Map<String, Object> providers = AuthenticationProviderFactory.getAvailableProviders();

        assertNotNull(providers);
        assertTrue(providers.containsKey("DEFAULT"));
        assertTrue(providers.containsKey("BEDROCK"));
        assertTrue(providers.containsKey("VERTEX_AI"));

        @SuppressWarnings("unchecked")
        Map<String, Object> defaultProvider = (Map<String, Object>) providers.get("DEFAULT");
        assertEquals("default", defaultProvider.get("code"));
        assertEquals("默认认证（API Key）", defaultProvider.get("description"));
        assertNotNull(defaultProvider.get("configured"));
    }

    @Test
    void testCreateProviderWithInvalidConfig() {
        Map<String, String> config = new HashMap<>();
        // 空配置应该抛出异常
        assertThrows(ClaudeCodeException.class, () ->
                AuthenticationProviderFactory.createDefaultProvider(config));
    }

    @Test
    void testCreateBedrockProviderWithInvalidCredentials() {
        Map<String, String> config = new HashMap<>();
        config.put("accessKey", "invalid-key");
        config.put("secretKey", "invalid-secret");
        config.put("region", "invalid-region");

        // 应该创建提供者但认证会失败
        assertDoesNotThrow(() -> {
            AuthenticationProvider provider = AuthenticationProviderFactory.createBedrockProvider(config);
            assertNotNull(provider);
        });
    }

    @Test
    void testCreateVertexAIProviderWithInvalidProject() {
        Map<String, String> config = new HashMap<>();
        config.put("projectId", "invalid_project_123"); // 无效格式
        config.put("location", "us-central1");

        // 验证创建提供者但认证会失败
        assertDoesNotThrow(() -> {
            AuthenticationProvider provider = AuthenticationProviderFactory.createVertexAIProvider(config);
            assertNotNull(provider);
            // 认证会失败，但创建不会抛出异常
            assertFalse(provider.isAuthenticated());
        });
    }
}