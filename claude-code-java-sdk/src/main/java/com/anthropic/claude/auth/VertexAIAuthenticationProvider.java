package com.anthropic.claude.auth;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google Vertex AI 认证提供者
 * 支持Google Cloud凭证管理和Vertex AI服务集成
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class VertexAIAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIAuthenticationProvider.class);

    private final String projectId;
    private final String location;
    private final String serviceAccountKey;
    private final String credentialsPath;
    private final Map<String, Object> credentials = new ConcurrentHashMap<>();
    private volatile boolean authenticated = false;
    private volatile LocalDateTime lastRefresh;
    private volatile String accessToken;

    public VertexAIAuthenticationProvider(String projectId, String location, String serviceAccountKey) {
        this(projectId, location, serviceAccountKey, null);
    }

    public VertexAIAuthenticationProvider(String projectId, String location,
                                        String serviceAccountKey, String credentialsPath) {
        this.projectId = validateRequired(projectId, "Google Cloud Project ID");
        this.location = validateRequired(location, "Google Cloud Location");
        this.serviceAccountKey = serviceAccountKey;
        this.credentialsPath = credentialsPath;

        initializeCredentials();
        logger.info("Vertex AI认证提供者已初始化 - Project: {}, Location: {}", projectId, location);
    }

    /**
     * 从环境变量创建Vertex AI认证提供者
     *
     * @return VertexAIAuthenticationProvider实例
     */
    public static VertexAIAuthenticationProvider fromEnvironment() {
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        String location = System.getenv("GOOGLE_CLOUD_LOCATION");
        String serviceAccountKey = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_PATH");

        if (projectId == null) {
            throw new ClaudeCodeException("Google Cloud项目ID环境变量未设置 (GOOGLE_CLOUD_PROJECT)");
        }

        if (location == null) {
            location = "us-central1"; // 默认位置
            logger.warn("未设置GOOGLE_CLOUD_LOCATION，使用默认位置: {}", location);
        }

        return new VertexAIAuthenticationProvider(projectId, location, serviceAccountKey, credentialsPath);
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated && accessToken != null;
    }

    @Override
    public String getAuthHeader() {
        if (!isAuthenticated()) {
            throw new ClaudeCodeException("Vertex AI认证未完成");
        }

        return "Bearer " + accessToken;
    }

    @Override
    public void refreshAuth() {
        logger.info("刷新Vertex AI认证");

        try {
            // 验证Google Cloud凭证
            validateGoogleCloudCredentials();

            // 获取访问令牌
            accessToken = obtainAccessToken();

            // 更新认证状态
            authenticated = true;
            lastRefresh = LocalDateTime.now();

            logger.info("Vertex AI认证刷新成功");

        } catch (Exception e) {
            authenticated = false;
            accessToken = null;
            logger.error("Vertex AI认证刷新失败", e);
            throw new ClaudeCodeException("Vertex AI认证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();

        if (isAuthenticated()) {
            headers.put("Authorization", getAuthHeader());
            headers.put("X-Goog-User-Project", projectId);
            headers.put("Content-Type", "application/json");
        }

        return headers;
    }

    /**
     * 获取Google Cloud项目ID
     *
     * @return 项目ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * 获取Google Cloud位置
     *
     * @return 位置
     */
    public String getLocation() {
        return location;
    }

    /**
     * 获取Vertex AI端点URL
     *
     * @param modelId 模型ID
     * @return 端点URL
     */
    public String getEndpointUrl(String modelId) {
        return String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models/%s:predict",
                location, projectId, location, modelId);
    }

    /**
     * 检查是否需要刷新认证
     *
     * @return 是否需要刷新
     */
    public boolean needsRefresh() {
        if (!authenticated || lastRefresh == null || accessToken == null) {
            return true;
        }

        // Google Cloud访问令牌通常有效期1小时，提前10分钟刷新
        return lastRefresh.isBefore(LocalDateTime.now().minusMinutes(50));
    }

    /**
     * 获取凭证信息（用于调试）
     *
     * @return 凭证摘要信息
     */
    public Map<String, Object> getCredentialsSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        summary.put("authenticated", authenticated);
        summary.put("projectId", projectId);
        summary.put("location", location);
        summary.put("hasServiceAccountKey", serviceAccountKey != null && !serviceAccountKey.isEmpty());
        summary.put("hasCredentialsPath", credentialsPath != null && !credentialsPath.isEmpty());
        summary.put("hasAccessToken", accessToken != null && !accessToken.isEmpty());
        summary.put("lastRefresh", lastRefresh);
        return summary;
    }

    /**
     * 初始化凭证
     */
    private void initializeCredentials() {
        credentials.put("projectId", projectId);
        credentials.put("location", location);
        if (serviceAccountKey != null) {
            credentials.put("serviceAccountKey", serviceAccountKey);
        }
        if (credentialsPath != null) {
            credentials.put("credentialsPath", credentialsPath);
        }

        // 尝试初始认证
        try {
            refreshAuth();
        } catch (Exception e) {
            logger.warn("初始认证失败，需要手动刷新: {}", e.getMessage());
        }
    }

    /**
     * 验证Google Cloud凭证
     */
    private void validateGoogleCloudCredentials() {
        logger.debug("验证Google Cloud凭证有效性");

        if (!isValidProjectId(projectId)) {
            throw new ClaudeCodeException("Google Cloud项目ID格式无效: " + projectId);
        }

        if (!isValidLocation(location)) {
            throw new ClaudeCodeException("Google Cloud位置格式无效: " + location);
        }

        // 检查是否有有效的认证方式
        if ((serviceAccountKey == null || serviceAccountKey.isEmpty()) &&
            (credentialsPath == null || credentialsPath.isEmpty())) {

            // 尝试使用默认凭证
            logger.info("使用Google Cloud默认凭证");
        }

        logger.debug("Google Cloud凭证验证通过");
    }

    /**
     * 获取访问令牌
     */
    private String obtainAccessToken() {
        logger.debug("获取Google Cloud访问令牌");

        // 在实际实现中，这里会：
        // 1. 使用Google Auth库获取访问令牌
        // 2. 支持多种认证方式：服务账户密钥、ADC等
        // 3. 处理令牌刷新逻辑

        // 这是一个模拟实现
        String token = generateMockAccessToken();
        logger.debug("访问令牌获取成功");
        return token;
    }

    /**
     * 生成模拟访问令牌（仅用于测试）
     */
    private String generateMockAccessToken() {
        // 在实际实现中，这会是真实的OAuth2访问令牌
        long timestamp = System.currentTimeMillis();
        return "ya29.mock_token_" + timestamp;
    }

    /**
     * 验证必需参数
     */
    private String validateRequired(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new ClaudeCodeException(name + "不能为空");
        }
        return value.trim();
    }

    /**
     * 验证Google Cloud项目ID格式
     */
    private boolean isValidProjectId(String projectId) {
        // Google Cloud项目ID格式：6-30个字符，小写字母、数字和连字符
        return projectId != null && projectId.matches("^[a-z][a-z0-9-]{5,29}$");
    }

    /**
     * 验证Google Cloud位置格式
     */
    private boolean isValidLocation(String location) {
        // 常见的Google Cloud位置格式
        return location != null && location.matches("^[a-z0-9-]+$");
    }

    @Override
    public String toString() {
        return String.format("VertexAIAuthenticationProvider{projectId='%s', location='%s', authenticated=%s}",
                projectId, location, authenticated);
    }
}