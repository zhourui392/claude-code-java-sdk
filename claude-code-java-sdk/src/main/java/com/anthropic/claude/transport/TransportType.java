package com.anthropic.claude.transport;

/**
 * 传输层类型枚举
 *
 * <p>定义 Claude Agent SDK 支持的传输层类型</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public enum TransportType {
    /**
     * CLI 进程传输（默认）
     * <p>通过启动 Claude CLI 进程进行通信</p>
     */
    CLI_PROCESS,

    /**
     * HTTP API 传输
     * <p>直接通过 HTTP API 与 Claude 通信</p>
     */
    HTTP_API,

    /**
     * WebSocket 传输
     * <p>通过 WebSocket 进行实时双向通信</p>
     */
    WEBSOCKET,

    /**
     * 用户自定义传输
     * <p>允许用户实现自己的传输层</p>
     */
    CUSTOM
}
