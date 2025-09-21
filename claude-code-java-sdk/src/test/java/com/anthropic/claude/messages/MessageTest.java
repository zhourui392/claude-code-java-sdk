package com.anthropic.claude.messages;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testBuilderWithAllOptions() {
        String id = "test-id";
        String content = "test content";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");
        Instant timestamp = Instant.now();

        Message message = Message.builder()
                .id(id)
                .type(MessageType.TEXT)
                .content(content)
                .metadata(metadata)
                .timestamp(timestamp)
                .build();

        assertEquals(id, message.getId());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals(content, message.getContent());
        assertEquals("value", message.getMetadata().get("key"));
        assertEquals(timestamp, message.getInstantTimestamp());
    }

    @Test
    void testBuilderWithDefaults() {
        String content = "test content";

        Message message = Message.builder()
                .content(content)
                .build();

        assertNotNull(message.getId());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals(content, message.getContent());
        assertTrue(message.getMetadata().isEmpty());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testJsonConstructor() {
        String id = "test-id";
        String type = "TEXT";
        String content = "test content";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");
        Instant timestamp = Instant.now();

        Message message = new Message(id, type, null, content, metadata, timestamp);

        assertEquals(id, message.getId());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals(content, message.getContent());
        assertEquals("value", message.getMetadata().get("key"));
        assertEquals(timestamp, message.getInstantTimestamp());
    }

    @Test
    void testJsonConstructorWithNulls() {
        String content = "test content";

        Message message = new Message(null, null, null, content, null, null);

        assertNotNull(message.getId());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals(content, message.getContent());
        assertTrue(message.getMetadata().isEmpty());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testTextFactory() {
        String content = "Hello World";

        Message message = Message.text(content);

        assertNotNull(message.getId());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals(content, message.getContent());
        assertTrue(message.getMetadata().isEmpty());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testToolFactory() {
        String toolName = "test-tool";
        Map<String, Object> args = new HashMap<>();
        args.put("param1", "value1");

        Message message = Message.tool(toolName, args);

        assertNotNull(message.getId());
        assertEquals(MessageType.TOOL_CALL, message.getType());
        assertEquals(toolName, message.getContent());
        assertEquals(toolName, message.getMetadata().get("tool_name"));
        assertEquals(args, message.getMetadata().get("tool_args"));
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testErrorFactory() {
        String errorMessage = "Something went wrong";

        Message message = Message.error(errorMessage);

        assertNotNull(message.getId());
        assertEquals(MessageType.ERROR, message.getType());
        assertEquals(errorMessage, message.getContent());
        assertTrue(message.getMetadata().isEmpty());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testSystemFactory() {
        String systemMessage = "System initialized";

        Message message = Message.system(systemMessage);

        assertNotNull(message.getId());
        assertEquals(MessageType.SYSTEM, message.getType());
        assertEquals(systemMessage, message.getContent());
        assertTrue(message.getMetadata().isEmpty());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void testAddMetadata() {
        Message message = Message.builder()
                .content("test")
                .addMetadata("key1", "value1")
                .addMetadata("key2", 42)
                .build();

        Map<String, Object> metadata = message.getMetadata();
        assertEquals("value1", metadata.get("key1"));
        assertEquals(42, metadata.get("key2"));
    }

    @Test
    void testMetadataImmutability() {
        Map<String, Object> originalMetadata = new HashMap<>();
        originalMetadata.put("original", "value");

        Message message = Message.builder()
                .content("test")
                .metadata(originalMetadata)
                .build();

        originalMetadata.put("modified", "new-value");

        Map<String, Object> messageMetadata = message.getMetadata();
        assertFalse(messageMetadata.containsKey("modified"));

        Map<String, Object> messageMetadata2 = message.getMetadata();
        assertNotSame(messageMetadata, messageMetadata2);
    }

    @Test
    void testToJsonAndFromJson() {
        Message originalMessage = Message.builder()
                .id("test-id")
                .type(MessageType.TEXT)
                .content("test content")
                .addMetadata("key", "value")
                .build();

        String json = originalMessage.toJson();
        System.out.println("Generated JSON: " + json);  // Debug output
        assertNotNull(json);
        assertTrue(json.contains("test-id"));
        // MessageType序列化为小写字符串
        assertTrue(json.contains("text"));
        assertTrue(json.contains("test content"));

        Message parsedMessage = Message.fromJson(json);
        assertEquals(originalMessage.getId(), parsedMessage.getId());
        assertEquals(originalMessage.getType(), parsedMessage.getType());
        assertEquals(originalMessage.getContent(), parsedMessage.getContent());
        assertEquals("value", parsedMessage.getMetadata().get("key"));
    }

    @Test
    void testFromJsonWithInvalidJson() {
        String invalidJson = "not json";

        assertThrows(RuntimeException.class, () -> Message.fromJson(invalidJson));
    }

    @Test
    void testToJsonFailure() {
        Message message = Message.builder()
                .content("test")
                .build();

        assertDoesNotThrow(() -> message.toJson());
    }

    @Test
    void testToString() {
        Message message = Message.builder()
                .id("test-id")
                .type(MessageType.TEXT)
                .content("test content")
                .build();

        String toString = message.toString();
        assertTrue(toString.contains("test-id"));
        assertTrue(toString.contains("TEXT"));
        assertTrue(toString.contains("test content"));
    }
}