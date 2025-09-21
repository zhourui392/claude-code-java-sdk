package com.anthropic.claude.query;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class QueryRequest {
    private final String prompt;
    private final String[] tools;
    private final String context;
    private final Integer maxTokens;
    private final Double temperature;
    private final Duration timeout;
    private final Map<String, Object> metadata;
    private final String resumeSessionId;
    private final boolean continueLastSession;

    private QueryRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.tools = builder.tools;
        this.context = builder.context;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.timeout = builder.timeout;
        this.metadata = builder.metadata;
        this.resumeSessionId = builder.resumeSessionId;
        this.continueLastSession = builder.continueLastSession;
    }

    public String getPrompt() {
        return prompt;
    }

    public String[] getTools() {
        return tools != null ? tools.clone() : new String[0];
    }

    public String getContext() {
        return context;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public String getResumeSessionId() {
        return resumeSessionId;
    }

    public boolean isContinueLastSession() {
        return continueLastSession;
    }

    public static Builder builder(String prompt) {
        return new Builder(prompt);
    }

    public static class Builder {
        private final String prompt;
        private String[] tools;
        private String context;
        private Integer maxTokens;
        private Double temperature;
        private Duration timeout;
        private Map<String, Object> metadata = new HashMap<>();
        private String resumeSessionId;
        private boolean continueLastSession;

        private Builder(String prompt) {
            this.prompt = prompt;
        }

        public Builder withTools(String... tools) {
            this.tools = tools;
            return this;
        }

        public Builder withContext(String context) {
            this.context = context;
            return this;
        }

        public Builder withMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder withTemperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder withTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder withResumeSessionId(String sessionId) {
            this.resumeSessionId = sessionId;
            return this;
        }

        public Builder withContinueLastSession(boolean continueLastSession) {
            this.continueLastSession = continueLastSession;
            return this;
        }

        public QueryRequest build() {
            return new QueryRequest(this);
        }
    }
}