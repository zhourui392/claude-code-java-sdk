package com.anthropic.claude.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private final String id;
    private final MessageType type;
    private final String subtype;
    private final String content;
    private final Map<String, Object> metadata;
    private final Instant timestamp;

    @JsonCreator
    public Message(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("subtype") String subtype,
            @JsonProperty("content") String content,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("timestamp") Object timestamp) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.type = MessageType.fromValue(type);
        this.subtype = subtype;
        this.content = content;
        this.metadata = metadata != null ? metadata : new HashMap<>();

        if (timestamp instanceof String) {
            this.timestamp = Instant.parse((String) timestamp);
        } else if (timestamp instanceof Instant) {
            this.timestamp = (Instant) timestamp;
        } else {
            this.timestamp = Instant.now();
        }
    }

    private Message(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.type = builder.type;
        this.subtype = builder.subtype;
        this.content = builder.content;
        this.metadata = builder.metadata;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    }

    public static Message fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            mapper.registerModule(javaTimeModule);
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
            mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
            return mapper.readValue(json, Message.class);
        } catch (Exception e) {
            throw new RuntimeException("解析JSON消息失败", e);
        }
    }

    public static Message text(String content) {
        return builder()
                .type(MessageType.TEXT)
                .content(content)
                .build();
    }

    public static Message tool(String toolName, Map<String, Object> args) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tool_name", toolName);
        metadata.put("tool_args", args);

        return builder()
                .type(MessageType.TOOL_CALL)
                .content(toolName)
                .metadata(metadata)
                .build();
    }

    public static Message error(String errorMessage) {
        return builder()
                .type(MessageType.ERROR)
                .content(errorMessage)
                .build();
    }

    public static Message system(String systemMessage) {
        return builder()
                .type(MessageType.SYSTEM)
                .content(systemMessage)
                .build();
    }

    public Message(MessageType type, String content) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.subtype = null;
        this.content = content;
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
    }

    public Message(MessageType type, String content, java.time.LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.subtype = null;
        this.content = content;
        this.metadata = new HashMap<>();
        this.timestamp = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            mapper.registerModule(javaTimeModule);
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("序列化消息为JSON失败", e);
        }
    }

    public String getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public java.time.LocalDateTime getTimestamp() {
        return timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }

    public Instant getInstantTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public String getJsonTimestamp() {
        return timestamp.toString();
    }

    @Override
    public String toString() {
        return String.format("Message{id='%s', type=%s, content='%s', timestamp=%s}",
                id, type, content, timestamp);
    }

    public static class Builder {
        private String id;
        private MessageType type = MessageType.TEXT;
        private String subtype;
        private String content;
        private Map<String, Object> metadata = new HashMap<>();
        private Instant timestamp;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder subtype(String subtype) {
            this.subtype = subtype;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}