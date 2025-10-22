package com.anthropic.claude.exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Claude Agent SDK 基础异常类
 *
 * <p>这是所有 Claude Agent SDK 异常的基类，提供错误代码和上下文信息。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * throw new ClaudeAgentException("INVALID_CONFIG", "API key is required");
 *
 * throw new ClaudeAgentException(
 *     "AUTH_FAILED",
 *     "Authentication failed",
 *     Map.of("provider", "bedrock", "region", "us-east-1")
 * );
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class ClaudeAgentException extends RuntimeException {

    /**
     * 错误代码，用于标识错误类型
     */
    private final String errorCode;

    /**
     * 错误上下文，包含额外的调试信息
     */
    private final Map<String, Object> context;

    /**
     * 创建异常（使用默认错误代码）
     *
     * @param message 错误消息
     */
    public ClaudeAgentException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.context = Collections.emptyMap();
    }

    /**
     * 创建异常（带原因）
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public ClaudeAgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.context = Collections.emptyMap();
    }

    /**
     * 创建异常（带错误代码）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public ClaudeAgentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    /**
     * 创建异常（带错误代码和原因）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public ClaudeAgentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    /**
     * 创建异常（带错误代码和上下文）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param context 错误上下文
     */
    public ClaudeAgentException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? Collections.unmodifiableMap(new HashMap<>(context)) : Collections.emptyMap();
    }

    /**
     * 创建异常（完整参数）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     * @param context 错误上下文
     */
    public ClaudeAgentException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context != null ? Collections.unmodifiableMap(new HashMap<>(context)) : Collections.emptyMap();
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误上下文
     *
     * @return 不可变的错误上下文 Map
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * 检查是否包含指定的上下文键
     *
     * @param key 键名
     * @return 是否包含
     */
    public boolean hasContextKey(String key) {
        return context.containsKey(key);
    }

    /**
     * 获取上下文值
     *
     * @param key 键名
     * @return 值，如果不存在返回 null
     */
    public Object getContextValue(String key) {
        return context.get(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClaudeAgentException[").append(errorCode).append("]: ").append(getMessage());

        if (!context.isEmpty()) {
            sb.append(" (context: ").append(context).append(")");
        }

        return sb.toString();
    }
}
