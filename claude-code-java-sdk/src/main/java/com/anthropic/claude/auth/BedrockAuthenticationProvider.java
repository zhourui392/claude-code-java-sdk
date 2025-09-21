package com.anthropic.claude.auth;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Amazon Bedrock 认证提供者
 * 支持AWS凭证管理和Bedrock服务集成
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class BedrockAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(BedrockAuthenticationProvider.class);

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String sessionToken;
    private final Map<String, Object> credentials = new ConcurrentHashMap<>();
    private volatile boolean authenticated = false;
    private volatile LocalDateTime lastRefresh;

    public BedrockAuthenticationProvider(String accessKey, String secretKey, String region) {
        this(accessKey, secretKey, region, null);
    }

    public BedrockAuthenticationProvider(String accessKey, String secretKey, String region, String sessionToken) {
        this.accessKey = validateRequired(accessKey, "AWS Access Key");
        this.secretKey = validateRequired(secretKey, "AWS Secret Key");
        this.region = validateRequired(region, "AWS Region");
        this.sessionToken = sessionToken;

        initializeCredentials();
        logger.info("Bedrock认证提供者已初始化 - Region: {}", region);
    }

    /**
     * 从环境变量创建Bedrock认证提供者
     *
     * @return BedrockAuthenticationProvider实例
     */
    public static BedrockAuthenticationProvider fromEnvironment() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String region = System.getenv("AWS_DEFAULT_REGION");
        String sessionToken = System.getenv("AWS_SESSION_TOKEN");

        if (accessKey == null || secretKey == null) {
            throw new ClaudeCodeException("AWS凭证环境变量未设置 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)");
        }

        if (region == null) {
            region = "us-east-1"; // 默认区域
            logger.warn("未设置AWS_DEFAULT_REGION，使用默认区域: {}", region);
        }

        return new BedrockAuthenticationProvider(accessKey, secretKey, region, sessionToken);
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public String getAuthHeader() {
        if (!authenticated) {
            throw new ClaudeCodeException("Bedrock认证未完成");
        }

        // 为Bedrock生成AWS Signature V4认证头
        return generateAwsSignature();
    }

    @Override
    public void refreshAuth() {
        logger.info("刷新Bedrock认证");

        try {
            // 验证AWS凭证
            validateAwsCredentials();

            // 更新认证状态
            authenticated = true;
            lastRefresh = LocalDateTime.now();

            logger.info("Bedrock认证刷新成功");

        } catch (Exception e) {
            authenticated = false;
            logger.error("Bedrock认证刷新失败", e);
            throw new ClaudeCodeException("Bedrock认证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();

        if (authenticated) {
            headers.put("Authorization", getAuthHeader());
            headers.put("X-Amz-Date", getCurrentAwsDate());
            headers.put("X-Amz-Region", region);

            if (sessionToken != null && !sessionToken.isEmpty()) {
                headers.put("X-Amz-Security-Token", sessionToken);
            }
        }

        return headers;
    }

    /**
     * 获取AWS区域
     *
     * @return AWS区域
     */
    public String getRegion() {
        return region;
    }

    /**
     * 获取Bedrock模型ARN
     *
     * @param modelId 模型ID
     * @return 模型ARN
     */
    public String getModelArn(String modelId) {
        return String.format("arn:aws:bedrock:%s::foundation-model/%s", region, modelId);
    }

    /**
     * 检查是否需要刷新认证
     *
     * @return 是否需要刷新
     */
    public boolean needsRefresh() {
        if (!authenticated || lastRefresh == null) {
            return true;
        }

        // AWS凭证通常有效期较长，这里设置1小时的刷新间隔
        return lastRefresh.isBefore(LocalDateTime.now().minusHours(1));
    }

    /**
     * 获取凭证信息（用于调试）
     *
     * @return 凭证摘要信息
     */
    public Map<String, Object> getCredentialsSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        summary.put("authenticated", authenticated);
        summary.put("region", region);
        summary.put("hasSessionToken", sessionToken != null && !sessionToken.isEmpty());
        summary.put("lastRefresh", lastRefresh);
        summary.put("accessKeyPreview", accessKey.substring(0, Math.min(4, accessKey.length())) + "***");
        return summary;
    }

    /**
     * 初始化凭证
     */
    private void initializeCredentials() {
        credentials.put("accessKey", accessKey);
        credentials.put("secretKey", secretKey);
        credentials.put("region", region);
        if (sessionToken != null) {
            credentials.put("sessionToken", sessionToken);
        }

        // 尝试初始认证
        try {
            refreshAuth();
        } catch (Exception e) {
            logger.warn("初始认证失败，需要手动刷新: {}", e.getMessage());
        }
    }

    /**
     * 验证AWS凭证
     */
    private void validateAwsCredentials() {
        // 在实际实现中，这里会调用AWS STS服务验证凭证
        // 例如：调用 GetCallerIdentity 来验证凭证有效性
        logger.debug("验证AWS凭证有效性");

        // 简单的格式验证
        if (!isValidAccessKey(accessKey)) {
            throw new ClaudeCodeException("AWS Access Key格式无效");
        }

        if (!isValidSecretKey(secretKey)) {
            throw new ClaudeCodeException("AWS Secret Key格式无效");
        }

        if (!isValidRegion(region)) {
            throw new ClaudeCodeException("AWS Region格式无效: " + region);
        }

        logger.debug("AWS凭证格式验证通过");
    }

    /**
     * 生成AWS Signature V4认证头
     */
    private String generateAwsSignature() {
        // 在实际实现中，这里会生成完整的AWS Signature V4签名
        // 这是一个简化的实现
        String timestamp = getCurrentAwsDate();
        String credentialScope = String.format("%s/%s/bedrock/aws4_request",
                timestamp.substring(0, 8), region);

        return String.format("AWS4-HMAC-SHA256 Credential=%s/%s, SignedHeaders=host;x-amz-date, Signature=...",
                accessKey, credentialScope);
    }

    /**
     * 获取当前AWS日期格式
     */
    private String getCurrentAwsDate() {
        return java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
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
     * 验证AWS Access Key格式
     */
    private boolean isValidAccessKey(String accessKey) {
        return accessKey != null && accessKey.matches("^[A-Z0-9]{20}$");
    }

    /**
     * 验证AWS Secret Key格式
     */
    private boolean isValidSecretKey(String secretKey) {
        return secretKey != null && secretKey.length() == 40;
    }

    /**
     * 验证AWS Region格式
     */
    private boolean isValidRegion(String region) {
        return region != null && region.matches("^[a-z0-9-]+$");
    }

    @Override
    public String toString() {
        return String.format("BedrockAuthenticationProvider{region='%s', authenticated=%s}",
                region, authenticated);
    }
}