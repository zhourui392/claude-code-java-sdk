package com.anthropic.claude.pty;

/**
 * Claude CLI状态枚举
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public enum ClaudeState {
    STARTING("启动中"),
    READY("就绪，等待输入"),
    PROCESSING("处理中"),
    STREAMING("流式输出中"),
    ERROR("错误状态"),
    USAGE_LIMIT("使用限制"),
    AUTH_REQUIRED("需要认证"),
    SESSION_RESTORED("会话恢复完成"),
    COMPLETED("响应完成"),
    WAITING_INPUT("等待用户输入"),
    INTERRUPTED("中断状态");

    private final String description;

    ClaudeState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}