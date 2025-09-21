package com.anthropic.claude.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 消息模型类
 *
 * @author Claude Code Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private MessageType type;

    @JsonProperty("role")
    private Role role;

    @JsonProperty("content")
    private String content;

    @JsonProperty("model")
    private String model;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("usage")
    private Usage usage;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        @JsonProperty("message")
        MESSAGE,
        @JsonProperty("error")
        ERROR,
        @JsonProperty("system")
        SYSTEM,
        @JsonProperty("tool_use")
        TOOL_USE,
        @JsonProperty("tool_result")
        TOOL_RESULT
    }

    /**
     * 角色枚举
     */
    public enum Role {
        @JsonProperty("user")
        USER,
        @JsonProperty("assistant")
        ASSISTANT,
        @JsonProperty("system")
        SYSTEM,
        @JsonProperty("tool")
        TOOL
    }

    /**
     * Token使用统计
     */
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
               type == message.type &&
               role == message.role &&
               Objects.equals(content, message.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, role, content);
    }

    @Override
    public String toString() {
        return "Message{" +
               "id='" + id + '\'' +
               ", type=" + type +
               ", role=" + role +
               ", content='" + content + '\'' +
               ", model='" + model + '\'' +
               '}';
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private final Message message = new Message();

        public Builder id(String id) {
            message.id = id;
            return this;
        }

        public Builder type(MessageType type) {
            message.type = type;
            return this;
        }

        public Builder role(Role role) {
            message.role = role;
            return this;
        }

        public Builder content(String content) {
            message.content = content;
            return this;
        }

        public Builder model(String model) {
            message.model = model;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            message.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            message.metadata = metadata;
            return this;
        }

        public Builder usage(Usage usage) {
            message.usage = usage;
            return this;
        }

        public Message build() {
            if (message.timestamp == null) {
                message.timestamp = Instant.now();
            }
            return message;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}