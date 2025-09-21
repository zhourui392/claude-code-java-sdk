package com.claude.gui.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 消息模型类
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class Message {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String sender;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;

    public Message(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public String getFormattedTime() {
        return TIME_FORMATTER.format(timestamp);
    }

    // Getters and Setters

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}