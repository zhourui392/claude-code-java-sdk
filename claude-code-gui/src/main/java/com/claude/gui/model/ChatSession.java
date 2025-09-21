package com.claude.gui.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 会话模型类，用于管理Claude Code会话
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class ChatSession {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String name;
    private LocalDateTime createdTime;
    private LocalDateTime lastActiveTime;
    private List<Message> messages;
    private boolean isActive;

    /**
     * 构造函数
     */
    public ChatSession() {
        this.id = UUID.randomUUID().toString();
        this.createdTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.isActive = false;
        this.name = "新会话 " + FORMATTER.format(createdTime);
    }

    /**
     * 构造函数，指定会话名称
     */
    public ChatSession(String name) {
        this();
        this.name = name;
    }

    /**
     * 添加消息到会话
     */
    public void addMessage(String sender, String content, MessageType type) {
        Message message = new Message(sender, content, type);
        messages.add(message);
        updateLastActiveTime();
    }

    /**
     * 获取会话预览（最近的几条消息）
     */
    public String getPreview() {
        if (messages.isEmpty()) {
            return "暂无对话";
        }

        // 获取最后一条用户或Claude的消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.getType() == MessageType.USER || msg.getType() == MessageType.CLAUDE) {
                String content = msg.getContent();
                return content.length() > 50 ? content.substring(0, 50) + "..." : content;
            }
        }

        return "暂无对话";
    }

    /**
     * 获取消息总数
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 获取格式化的创建时间
     */
    public String getFormattedCreatedTime() {
        return FORMATTER.format(createdTime);
    }

    /**
     * 获取格式化的最后活跃时间
     */
    public String getFormattedLastActiveTime() {
        return FORMATTER.format(lastActiveTime);
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void setMessages(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        updateLastActiveTime();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        if (active) {
            updateLastActiveTime();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChatSession that = (ChatSession) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}