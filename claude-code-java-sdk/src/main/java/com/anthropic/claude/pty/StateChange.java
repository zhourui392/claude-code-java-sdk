package com.anthropic.claude.pty;

import java.time.Instant;

/**
 * Claude状态变化事件
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class StateChange {
    private ClaudeState fromState;
    private ClaudeState toState;
    private String triggerContent;
    private ClaudeResponse response;
    private Instant timestamp;

    public StateChange(ClaudeState toState, String triggerContent) {
        this.toState = toState;
        this.triggerContent = triggerContent;
        this.timestamp = Instant.now();
    }

    public StateChange(ClaudeState fromState, ClaudeState toState, String triggerContent) {
        this(toState, triggerContent);
        this.fromState = fromState;
    }

    public StateChange(ClaudeState toState, ClaudeResponse response) {
        this.toState = toState;
        this.response = response;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public ClaudeState getFromState() {
        return fromState;
    }

    public void setFromState(ClaudeState fromState) {
        this.fromState = fromState;
    }

    public ClaudeState getToState() {
        return toState;
    }

    public void setToState(ClaudeState toState) {
        this.toState = toState;
    }

    public String getTriggerContent() {
        return triggerContent;
    }

    public void setTriggerContent(String triggerContent) {
        this.triggerContent = triggerContent;
    }

    public ClaudeResponse getResponse() {
        return response;
    }

    public void setResponse(ClaudeResponse response) {
        this.response = response;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("StateChange{%s -> %s, trigger='%s'}",
            fromState, toState, triggerContent);
    }
}