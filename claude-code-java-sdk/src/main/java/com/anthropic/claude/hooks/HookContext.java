package com.anthropic.claude.hooks;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class HookContext {
    private final String eventType;
    private final Map<String, Object> data;
    private final Instant timestamp;
    private final String sessionId;

    public HookContext(String eventType, Map<String, Object> data, String sessionId) {
        this.eventType = eventType;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.timestamp = Instant.now();
        this.sessionId = sessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void putData(String key, Object value) {
        data.put(key, value);
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public String toString() {
        return String.format("HookContext{eventType='%s', sessionId='%s', timestamp=%s, dataKeys=%s}",
                eventType, sessionId, timestamp, data.keySet());
    }
}