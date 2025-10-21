package com.anthropic.claude.transport;

import com.anthropic.claude.exceptions.ClaudeAgentException;

/**
 * 传输层异常
 *
 * <p>当传输层操作失败时抛出此异常</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class TransportException extends ClaudeAgentException {

    /**
     * 创建传输层异常
     *
     * @param message 错误消息
     */
    public TransportException(String message) {
        super("TRANSPORT_ERROR", message);
    }

    /**
     * 创建传输层异常（带原因）
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public TransportException(String message, Throwable cause) {
        super("TRANSPORT_ERROR", message, cause);
    }

    /**
     * 创建传输层异常（带错误代码）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public TransportException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建传输层异常（带错误代码和原因）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public TransportException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
