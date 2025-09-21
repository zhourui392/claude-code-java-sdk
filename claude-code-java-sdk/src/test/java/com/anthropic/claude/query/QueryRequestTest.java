package com.anthropic.claude.query;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryRequestTest {

    @Test
    void testBuilderWithAllOptions() {
        String prompt = "test prompt";
        String[] tools = {"tool1", "tool2"};
        String context = "test context";
        int maxTokens = 1000;
        double temperature = 0.7;
        Duration timeout = Duration.ofMinutes(5);

        QueryRequest request = QueryRequest.builder(prompt)
                .withTools(tools)
                .withContext(context)
                .withMaxTokens(maxTokens)
                .withTemperature(temperature)
                .withTimeout(timeout)
                .addMetadata("key1", "value1")
                .addMetadata("key2", 42)
                .build();

        assertEquals(prompt, request.getPrompt());
        assertArrayEquals(tools, request.getTools());
        assertEquals(context, request.getContext());
        assertEquals(maxTokens, request.getMaxTokens());
        assertEquals(temperature, request.getTemperature());
        assertEquals(timeout, request.getTimeout());
        assertEquals("value1", request.getMetadata().get("key1"));
        assertEquals(42, request.getMetadata().get("key2"));
    }

    @Test
    void testBuilderWithMinimalOptions() {
        String prompt = "test prompt";

        QueryRequest request = QueryRequest.builder(prompt).build();

        assertEquals(prompt, request.getPrompt());
        assertEquals(0, request.getTools().length);
        assertNull(request.getContext());
        assertNull(request.getMaxTokens());
        assertNull(request.getTemperature());
        assertNull(request.getTimeout());
        assertTrue(request.getMetadata().isEmpty());
    }

    @Test
    void testGetToolsReturnsCopy() {
        String[] tools = {"tool1", "tool2"};
        QueryRequest request = QueryRequest.builder("test")
                .withTools(tools)
                .build();

        String[] returnedTools = request.getTools();
        assertNotSame(tools, returnedTools);
        assertArrayEquals(tools, returnedTools);

        tools[0] = "modified";
        assertEquals("tool1", returnedTools[0]);
    }

    @Test
    void testGetToolsWithNullTools() {
        QueryRequest request = QueryRequest.builder("test").build();

        String[] tools = request.getTools();
        assertNotNull(tools);
        assertEquals(0, tools.length);
    }

    @Test
    void testGetMetadataReturnsCopy() {
        QueryRequest request = QueryRequest.builder("test")
                .addMetadata("key", "value")
                .build();

        Map<String, Object> metadata1 = request.getMetadata();
        Map<String, Object> metadata2 = request.getMetadata();

        assertNotSame(metadata1, metadata2);
        assertEquals(metadata1, metadata2);

        metadata1.put("new-key", "new-value");
        assertFalse(metadata2.containsKey("new-key"));
    }

    @Test
    void testWithToolsVariableArguments() {
        QueryRequest request = QueryRequest.builder("test")
                .withTools("tool1", "tool2", "tool3")
                .build();

        String[] tools = request.getTools();
        assertEquals(3, tools.length);
        assertEquals("tool1", tools[0]);
        assertEquals("tool2", tools[1]);
        assertEquals("tool3", tools[2]);
    }

    @Test
    void testWithToolsEmptyArray() {
        QueryRequest request = QueryRequest.builder("test")
                .withTools()
                .build();

        String[] tools = request.getTools();
        assertNotNull(tools);
        assertEquals(0, tools.length);
    }

    @Test
    void testBuilderState() {
        QueryRequest request1 = QueryRequest.builder("test prompt")
                .withContext("context1")
                .addMetadata("key1", "value1")
                .build();

        QueryRequest request2 = QueryRequest.builder("test prompt")
                .withContext("context2")
                .addMetadata("key2", "value2")
                .build();

        assertEquals("test prompt", request1.getPrompt());
        assertEquals("test prompt", request2.getPrompt());
        assertEquals("context1", request1.getContext());
        assertEquals("context2", request2.getContext());
        assertEquals("value1", request1.getMetadata().get("key1"));
        assertEquals("value2", request2.getMetadata().get("key2"));
        assertFalse(request1.getMetadata().containsKey("key2"));
        assertFalse(request2.getMetadata().containsKey("key1"));
    }

    @Test
    void testWithMaxTokensValidation() {
        QueryRequest request = QueryRequest.builder("test")
                .withMaxTokens(1000)
                .build();

        assertEquals(1000, request.getMaxTokens());
    }

    @Test
    void testWithTemperatureValidation() {
        QueryRequest request = QueryRequest.builder("test")
                .withTemperature(0.5)
                .build();

        assertEquals(0.5, request.getTemperature());
    }

    @Test
    void testWithTimeoutValidation() {
        Duration timeout = Duration.ofMinutes(10);
        QueryRequest request = QueryRequest.builder("test")
                .withTimeout(timeout)
                .build();

        assertEquals(timeout, request.getTimeout());
    }
}