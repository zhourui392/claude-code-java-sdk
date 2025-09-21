package com.anthropic.claude.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工具执行结果
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ToolExecutionResult {

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("result")
    private final Object result;

    @JsonProperty("error")
    private final String error;

    @JsonProperty("executionTime")
    private final long executionTimeMs;

    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;

    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    private ToolExecutionResult(Builder builder) {
        this.success = builder.success;
        this.result = builder.result;
        this.error = builder.error;
        this.executionTimeMs = builder.executionTimeMs;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.metadata = builder.metadata;
    }

    public static Builder success(Object result) {
        return new Builder().success(result);
    }

    public static Builder error(String error) {
        return new Builder().error(error);
    }

    public static Builder error(Throwable throwable) {
        return new Builder().error(throwable.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static class Builder {
        private boolean success;
        private Object result;
        private String error;
        private long executionTimeMs;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;

        private Builder success(Object result) {
            this.success = true;
            this.result = result;
            return this;
        }

        private Builder error(String error) {
            this.success = false;
            this.error = error;
            return this;
        }

        public Builder executionTime(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ToolExecutionResult build() {
            return new ToolExecutionResult(this);
        }
    }

    @Override
    public String toString() {
        return "ToolExecutionResult{" +
                "success=" + success +
                ", result=" + result +
                ", error='" + error + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                ", timestamp=" + timestamp +
                '}';
    }
}