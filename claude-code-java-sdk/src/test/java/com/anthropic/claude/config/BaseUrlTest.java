package com.anthropic.claude.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BaseUrlTest {

    @Test
    void testBaseUrlSupportInOptions() {
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-key")
                .baseUrl("https://api.packycode.com")
                .build();

        assertEquals("https://api.packycode.com", options.getBaseUrl());
        assertEquals("test-key", options.getApiKey());
    }

    @Test
    void testBaseUrlDefaultValue() {
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                .apiKey("test-key")
                .build();

        assertEquals("https://api.anthropic.com", options.getBaseUrl());
    }

    @Test
    void testConfigLoaderBaseUrlHandling() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("base.url", "https://custom-api.example.com");
        loader.setProperty("api.key", "test-key");

        String baseUrl = loader.getBaseUrl();
        assertEquals("https://custom-api.example.com", baseUrl);

        ClaudeCodeOptions options = loader.createOptions();
        assertEquals("https://custom-api.example.com", options.getBaseUrl());
    }

    @Test
    void testEnvironmentVariablesIncludeBaseUrl() {
        ConfigLoader loader = new ConfigLoader();
        loader.setProperty("base.url", "https://my-api.com");
        loader.setProperty("api.key", "test-key");

        Map<String, String> envVars = loader.getEnvironmentVariables();
        assertEquals("https://my-api.com", envVars.get("ANTHROPIC_BASE_URL"));
        assertEquals("test-key", envVars.get("ANTHROPIC_API_KEY"));
    }
}