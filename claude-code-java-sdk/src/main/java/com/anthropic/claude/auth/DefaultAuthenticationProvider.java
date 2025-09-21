package com.anthropic.claude.auth;

import java.util.HashMap;
import java.util.Map;

public class DefaultAuthenticationProvider implements AuthenticationProvider {
    private final String apiKey;

    public DefaultAuthenticationProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("x-api-key", apiKey);
            headers.put("anthropic-version", "2023-06-01");
        }
        return headers;
    }

    @Override
    public boolean isAuthenticated() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public void refreshAuth() {
        // 默认实现不需要刷新
    }
}