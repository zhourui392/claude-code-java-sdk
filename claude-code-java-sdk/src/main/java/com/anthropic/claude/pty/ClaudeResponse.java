package com.anthropic.claude.pty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Claude CLI响应封装
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ClaudeResponse {
    private ClaudeResponseType type;
    private String content;
    private Map<String, Object> metadata;
    private ClaudeState state;
    private String sessionId;
    private Instant timestamp;

    public ClaudeResponse() {
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
    }

    public ClaudeResponse(ClaudeResponseType type, String content) {
        this();
        this.type = type;
        this.content = content;
    }

    public ClaudeResponse(ClaudeResponseType type, String content, ClaudeState state) {
        this(type, content);
        this.state = state;
    }

    public enum ClaudeResponseType {
        TEXT_RESPONSE,      // 普通文本响应
        JSON_RESPONSE,      // JSON格式响应
        ERROR_RESPONSE,     // 错误响应
        STATUS_UPDATE,      // 状态更新
        SESSION_INFO,       // 会话信息
        STREAM_CHUNK,       // 流式数据块
        SYSTEM_MESSAGE      // 系统消息
    }

    // Getters and Setters
    public ClaudeResponseType getType() {
        return type;
    }

    public void setType(ClaudeResponseType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public ClaudeState getState() {
        return state;
    }

    public void setState(ClaudeState state) {
        this.state = state;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("ClaudeResponse{type=%s, state=%s, content='%s'}",
            type, state, content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null");
    }
}