package com.anthropic.claude.exceptions;

/**
 * Claude Code SDK 异常类（已弃用）
 *
 * <p><strong>⚠️ 已弃用：</strong>此类已在 v2.0.0 中弃用。请改用 {@link ClaudeAgentException}。</p>
 *
 * <p>此类现在继承自 {@link ClaudeAgentException}，保持向后兼容性。
 * 所有构造函数都委托给父类 ClaudeAgentException。</p>
 *
 * <h3>迁移示例</h3>
 * <pre>{@code
 * // v1.0.0 (已弃用)
 * throw new ClaudeCodeException("API_ERROR", "Request failed");
 *
 * // v2.0.0 (推荐)
 * throw new ClaudeAgentException("API_ERROR", "Request failed");
 *
 * // v2.0.0 with context (新功能)
 * throw new ClaudeAgentException(
 *     "API_ERROR",
 *     "Request failed",
 *     Map.of("statusCode", 500, "endpoint", "/v1/messages")
 * );
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 1.0.0
 * @deprecated 自 2.0.0 起弃用，请使用 {@link ClaudeAgentException}
 */
@Deprecated
public class ClaudeCodeException extends ClaudeAgentException {

    /**
     * 创建异常（使用默认错误代码）
     *
     * @param message 错误消息
     * @deprecated 使用 {@link ClaudeAgentException#ClaudeAgentException(String)}
     */
    @Deprecated
    public ClaudeCodeException(String message) {
        super(message);
    }

    /**
     * 创建异常（带原因）
     *
     * @param message 错误消息
     * @param cause 原因
     * @deprecated 使用 {@link ClaudeAgentException#ClaudeAgentException(String, Throwable)}
     */
    @Deprecated
    public ClaudeCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建异常（带错误代码）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @deprecated 使用 {@link ClaudeAgentException#ClaudeAgentException(String, String)}
     */
    @Deprecated
    public ClaudeCodeException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建异常（带错误代码和原因）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     * @deprecated 使用 {@link ClaudeAgentException#ClaudeAgentException(String, String, Throwable)}
     */
    @Deprecated
    public ClaudeCodeException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     * @deprecated 使用继承自 {@link ClaudeAgentException#getErrorCode()}
     */
    @Deprecated
    @Override
    public String getErrorCode() {
        return super.getErrorCode();
    }

    @Override
    public String toString() {
        return "ClaudeCodeException[" + getErrorCode() + "]: " + getMessage() + " [DEPRECATED]";
    }
}
