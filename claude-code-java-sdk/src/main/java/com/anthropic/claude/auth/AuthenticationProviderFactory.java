package com.anthropic.claude.auth;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证提供者工厂
 * 负责根据配置创建合适的认证提供者
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class AuthenticationProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderFactory.class);

    /**
     * 认证提供者类型枚举
     */
    public enum ProviderType {
        DEFAULT("default", "默认认证（API Key）"),
        BEDROCK("bedrock", "Amazon Bedrock"),
        VERTEX_AI("vertex", "Google Vertex AI");

        private final String code;
        private final String description;

        ProviderType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public static ProviderType fromCode(String code) {
            for (ProviderType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            throw new ClaudeCodeException("未知的认证提供者类型: " + code);
        }
    }

    /**
     * 从环境变量自动创建认证提供者
     *
     * @return 合适的认证提供者
     */
    public static AuthenticationProvider createFromEnvironment() {
        // 检查环境变量以确定使用哪种认证提供者
        String useBedrock = System.getenv("CLAUDE_CODE_USE_BEDROCK");
        String useVertex = System.getenv("CLAUDE_CODE_USE_VERTEX");

        if ("true".equalsIgnoreCase(useBedrock)) {
            logger.info("从环境变量检测到Bedrock配置，创建Bedrock认证提供者");
            return createBedrockProvider();
        }

        if ("true".equalsIgnoreCase(useVertex)) {
            logger.info("从环境变量检测到Vertex AI配置，创建Vertex AI认证提供者");
            return createVertexAIProvider();
        }

        // 默认使用API Key认证
        String apiKey = System.getenv("CLAUDE_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.info("使用默认API Key认证提供者");
            return new DefaultAuthenticationProvider(apiKey);
        }

        throw new ClaudeCodeException("未找到有效的认证配置，请设置环境变量：CLAUDE_API_KEY 或启用云服务认证");
    }

    /**
     * 根据类型创建认证提供者
     *
     * @param providerType 提供者类型
     * @param config 配置参数
     * @return 认证提供者
     */
    public static AuthenticationProvider createProvider(ProviderType providerType, Map<String, String> config) {
        logger.info("创建认证提供者: {}", providerType.getDescription());

        return switch (providerType) {
            case DEFAULT -> createDefaultProvider(config);
            case BEDROCK -> createBedrockProvider(config);
            case VERTEX_AI -> createVertexAIProvider(config);
        };
    }

    /**
     * 创建默认认证提供者
     */
    public static AuthenticationProvider createDefaultProvider() {
        String apiKey = System.getenv("CLAUDE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ClaudeCodeException("CLAUDE_API_KEY环境变量未设置");
        }
        return new DefaultAuthenticationProvider(apiKey);
    }

    /**
     * 创建默认认证提供者（带配置）
     */
    public static AuthenticationProvider createDefaultProvider(Map<String, String> config) {
        String apiKey = config.getOrDefault("apiKey", System.getenv("CLAUDE_API_KEY"));
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ClaudeCodeException("API Key未配置");
        }
        return new DefaultAuthenticationProvider(apiKey);
    }

    /**
     * 创建Bedrock认证提供者
     */
    public static AuthenticationProvider createBedrockProvider() {
        try {
            return BedrockAuthenticationProvider.fromEnvironment();
        } catch (Exception e) {
            logger.error("创建Bedrock认证提供者失败", e);
            throw new ClaudeCodeException("Bedrock认证提供者创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建Bedrock认证提供者（带配置）
     */
    public static AuthenticationProvider createBedrockProvider(Map<String, String> config) {
        try {
            String accessKey = config.getOrDefault("accessKey", System.getenv("AWS_ACCESS_KEY_ID"));
            String secretKey = config.getOrDefault("secretKey", System.getenv("AWS_SECRET_ACCESS_KEY"));
            String region = config.getOrDefault("region", System.getenv("AWS_DEFAULT_REGION"));
            String sessionToken = config.getOrDefault("sessionToken", System.getenv("AWS_SESSION_TOKEN"));

            if (accessKey == null || secretKey == null) {
                return BedrockAuthenticationProvider.fromEnvironment();
            }

            return new BedrockAuthenticationProvider(accessKey, secretKey, region, sessionToken);

        } catch (Exception e) {
            logger.error("创建Bedrock认证提供者失败", e);
            throw new ClaudeCodeException("Bedrock认证提供者创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建Vertex AI认证提供者
     */
    public static AuthenticationProvider createVertexAIProvider() {
        try {
            return VertexAIAuthenticationProvider.fromEnvironment();
        } catch (Exception e) {
            logger.error("创建Vertex AI认证提供者失败", e);
            throw new ClaudeCodeException("Vertex AI认证提供者创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建Vertex AI认证提供者（带配置）
     */
    public static AuthenticationProvider createVertexAIProvider(Map<String, String> config) {
        try {
            String projectId = config.getOrDefault("projectId", System.getenv("GOOGLE_CLOUD_PROJECT"));
            String location = config.getOrDefault("location", System.getenv("GOOGLE_CLOUD_LOCATION"));
            String serviceAccountKey = config.getOrDefault("serviceAccountKey",
                    System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
            String credentialsPath = config.getOrDefault("credentialsPath",
                    System.getenv("GOOGLE_APPLICATION_CREDENTIALS_PATH"));

            if (projectId == null) {
                return VertexAIAuthenticationProvider.fromEnvironment();
            }

            return new VertexAIAuthenticationProvider(projectId, location, serviceAccountKey, credentialsPath);

        } catch (Exception e) {
            logger.error("创建Vertex AI认证提供者失败", e);
            throw new ClaudeCodeException("Vertex AI认证提供者创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测并创建最佳认证提供者
     * 按优先级检测：Bedrock -> Vertex AI -> Default
     *
     * @return 检测到的认证提供者
     */
    public static AuthenticationProvider detectAndCreate() {
        logger.info("自动检测最佳认证提供者");

        // 1. 检查Bedrock环境
        if (isBedrockConfigured()) {
            logger.info("检测到Bedrock配置，使用Bedrock认证");
            return createBedrockProvider();
        }

        // 2. 检查Vertex AI环境
        if (isVertexAIConfigured()) {
            logger.info("检测到Vertex AI配置，使用Vertex AI认证");
            return createVertexAIProvider();
        }

        // 3. 默认使用API Key认证
        logger.info("使用默认API Key认证");
        return createDefaultProvider();
    }

    /**
     * 获取所有可用的认证提供者信息
     *
     * @return 提供者信息映射
     */
    public static Map<String, Object> getAvailableProviders() {
        Map<String, Object> providers = new ConcurrentHashMap<>();

        for (ProviderType type : ProviderType.values()) {
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("code", type.getCode());
            info.put("description", type.getDescription());
            info.put("configured", isProviderConfigured(type));
            providers.put(type.name(), info);
        }

        return providers;
    }

    /**
     * 检查Bedrock是否已配置
     */
    private static boolean isBedrockConfigured() {
        String useBedrock = System.getenv("CLAUDE_CODE_USE_BEDROCK");
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

        return "true".equalsIgnoreCase(useBedrock) ||
               (accessKey != null && !accessKey.isEmpty() &&
                secretKey != null && !secretKey.isEmpty());
    }

    /**
     * 检查Vertex AI是否已配置
     */
    private static boolean isVertexAIConfigured() {
        String useVertex = System.getenv("CLAUDE_CODE_USE_VERTEX");
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        String credentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

        return "true".equalsIgnoreCase(useVertex) ||
               (projectId != null && !projectId.isEmpty()) ||
               (credentials != null && !credentials.isEmpty());
    }

    /**
     * 检查指定提供者是否已配置
     */
    private static boolean isProviderConfigured(ProviderType type) {
        return switch (type) {
            case DEFAULT -> System.getenv("CLAUDE_API_KEY") != null;
            case BEDROCK -> isBedrockConfigured();
            case VERTEX_AI -> isVertexAIConfigured();
        };
    }

    /**
     * 创建认证提供者构建器
     *
     * @param type 提供者类型
     * @return 构建器
     */
    public static AuthProviderBuilder builder(ProviderType type) {
        return new AuthProviderBuilder(type);
    }

    /**
     * 认证提供者构建器
     */
    public static class AuthProviderBuilder {
        private final ProviderType type;
        private final Map<String, String> config = new ConcurrentHashMap<>();

        private AuthProviderBuilder(ProviderType type) {
            this.type = type;
        }

        public AuthProviderBuilder config(String key, String value) {
            config.put(key, value);
            return this;
        }

        public AuthProviderBuilder apiKey(String apiKey) {
            return config("apiKey", apiKey);
        }

        public AuthProviderBuilder awsCredentials(String accessKey, String secretKey, String region) {
            return config("accessKey", accessKey)
                    .config("secretKey", secretKey)
                    .config("region", region);
        }

        public AuthProviderBuilder googleCloudConfig(String projectId, String location) {
            return config("projectId", projectId)
                    .config("location", location);
        }

        public AuthenticationProvider build() {
            return createProvider(type, config);
        }
    }
}