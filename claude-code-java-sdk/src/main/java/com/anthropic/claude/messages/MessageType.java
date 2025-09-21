package com.anthropic.claude.messages;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    TEXT("text"),
    TOOL_CALL("tool_call"),
    TOOL_RESULT("tool_result"),
    ERROR("error"),
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    DEBUG("debug");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static MessageType fromValue(String value) {
        for (MessageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return TEXT;
    }
}