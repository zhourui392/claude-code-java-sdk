package com.anthropic.claude.auth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAuthenticationProviderTest {

    @Test
    void testConstructorWithValidApiKey() {
        String apiKey = "test-api-key";
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(apiKey);

        assertEquals(apiKey, provider.getApiKey());
    }

    @Test
    void testConstructorWithNullApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(null);

        assertNull(provider.getApiKey());
    }

    @Test
    void testConstructorWithEmptyApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider("");

        assertEquals("", provider.getApiKey());
    }

    @Test
    void testGetAuthHeadersWithValidApiKey() {
        String apiKey = "test-api-key";
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(apiKey);

        Map<String, String> headers = provider.getAuthHeaders();

        assertNotNull(headers);
        assertEquals(apiKey, headers.get("x-api-key"));
        assertEquals("2023-06-01", headers.get("anthropic-version"));
        assertEquals(2, headers.size());
    }

    @Test
    void testGetAuthHeadersWithNullApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(null);

        Map<String, String> headers = provider.getAuthHeaders();

        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    void testGetAuthHeadersWithEmptyApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider("");

        Map<String, String> headers = provider.getAuthHeaders();

        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    void testIsAuthenticatedWithValidApiKey() {
        String apiKey = "test-api-key";
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(apiKey);

        assertTrue(provider.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithNullApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(null);

        assertFalse(provider.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithEmptyApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider("");

        assertFalse(provider.isAuthenticated());
    }

    @Test
    void testIsAuthenticatedWithWhitespaceApiKey() {
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider("   ");

        assertFalse(provider.isAuthenticated());
    }

    @Test
    void testRefreshAuth() {
        String apiKey = "test-api-key";
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(apiKey);

        assertDoesNotThrow(() -> provider.refreshAuth());

        assertEquals(apiKey, provider.getApiKey());
        assertTrue(provider.isAuthenticated());
    }

    @Test
    void testGetAuthHeadersReturnsCopy() {
        String apiKey = "test-api-key";
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(apiKey);

        Map<String, String> headers1 = provider.getAuthHeaders();
        Map<String, String> headers2 = provider.getAuthHeaders();

        assertNotSame(headers1, headers2);
        assertEquals(headers1, headers2);
    }

    @Test
    void testGetAuthHeadersImmutable() {
        String apiKey = "test-api-key";
        DefaultAuthenticationProvider provider = new DefaultAuthenticationProvider(apiKey);

        Map<String, String> headers = provider.getAuthHeaders();

        assertDoesNotThrow(() -> headers.put("new-header", "new-value"));

        Map<String, String> freshHeaders = provider.getAuthHeaders();
        assertFalse(freshHeaders.containsKey("new-header"));
    }
}