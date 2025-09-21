package com.anthropic.claude.pty;

import java.util.ArrayList;
import java.util.List;

/**
 * Claude响应构建器
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ClaudeResponseBuilder {
    private StringBuilder contentBuffer = new StringBuilder();
    private List<String> errors = new ArrayList<>();
    private boolean isComplete = false;
    private ClaudeResponse.ClaudeResponseType responseType = ClaudeResponse.ClaudeResponseType.TEXT_RESPONSE;

    /**
     * 添加内容
     */
    public void appendContent(String content) {
        if (content != null && !isComplete) {
            contentBuffer.append(content).append("\n");
        }
    }

    /**
     * 设置错误
     */
    public void setError(String error) {
        if (error != null) {
            errors.add(error);
            responseType = ClaudeResponse.ClaudeResponseType.ERROR_RESPONSE;
        }
    }

    /**
     * 标记完成
     */
    public void complete() {
        isComplete = true;
    }

    /**
     * 构建响应
     */
    public ClaudeResponse build() {
        ClaudeResponse response = new ClaudeResponse();
        response.setType(responseType);

        if (!errors.isEmpty()) {
            response.setContent(String.join("\n", errors));
        } else {
            response.setContent(contentBuffer.toString().trim());
        }

        return response;
    }

    /**
     * 重置构建器
     */
    public void reset() {
        contentBuffer.setLength(0);
        errors.clear();
        isComplete = false;
        responseType = ClaudeResponse.ClaudeResponseType.TEXT_RESPONSE;
    }

    /**
     * 获取当前内容
     */
    public String getCurrentContent() {
        return contentBuffer.toString();
    }

    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 是否完成
     */
    public boolean isComplete() {
        return isComplete;
    }
}