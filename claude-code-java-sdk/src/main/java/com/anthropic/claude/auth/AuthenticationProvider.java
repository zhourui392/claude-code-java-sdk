package com.anthropic.claude.auth;

import java.util.Map;

public interface AuthenticationProvider {
    default String getApiKey() {
        return null; // 默认实现，多云认证可能不需要API Key
    }

    default String getAuthHeader() {
        return null; // 新增方法，用于获取认证头
    }

    Map<String, String> getAuthHeaders();

    boolean isAuthenticated();

    void refreshAuth();
}