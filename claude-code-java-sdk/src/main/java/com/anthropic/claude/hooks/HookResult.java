package com.anthropic.claude.hooks;

import java.util.HashMap;
import java.util.Map;

public class HookResult {
    private final boolean shouldContinue;
    private final Map<String, Object> modifiedData;
    private final String message;

    private HookResult(boolean shouldContinue, Map<String, Object> modifiedData, String message) {
        this.shouldContinue = shouldContinue;
        this.modifiedData = modifiedData != null ? new HashMap<>(modifiedData) : new HashMap<>();
        this.message = message;
    }

    public boolean shouldContinue() {
        return shouldContinue;
    }

    public Map<String, Object> getModifiedData() {
        return new HashMap<>(modifiedData);
    }

    public String getMessage() {
        return message;
    }

    public static HookResult proceed() {
        return new HookResult(true, null, null);
    }

    public static HookResult proceed(Map<String, Object> modifiedData) {
        return new HookResult(true, modifiedData, null);
    }

    public static HookResult proceed(String message) {
        return new HookResult(true, null, message);
    }

    public static HookResult proceed(Map<String, Object> modifiedData, String message) {
        return new HookResult(true, modifiedData, message);
    }

    public static HookResult stop(String reason) {
        return new HookResult(false, null, reason);
    }

    public static HookResult stop(Map<String, Object> modifiedData, String reason) {
        return new HookResult(false, modifiedData, reason);
    }

    @Override
    public String toString() {
        return String.format("HookResult{shouldContinue=%s, message='%s', hasModifiedData=%s}",
                shouldContinue, message, !modifiedData.isEmpty());
    }
}