package com.anthropic.claude.messages;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MessageParserTest {

    private MessageParser parser;

    @BeforeEach
    void setUp() {
        parser = new MessageParser();
    }

    @Test
    void testParseValidMessage() throws ClaudeCodeException {
        String json = "{\"id\":\"test-id\",\"type\":\"TEXT\",\"content\":\"test content\",\"metadata\":{},\"timestamp\":\"2024-01-01T00:00:00Z\"}";

        Message message = parser.parseMessage(json);

        assertNotNull(message);
        assertEquals("test-id", message.getId());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals("test content", message.getContent());
    }

    @Test
    void testParseMessageWithNullInput() {
        assertThrows(ClaudeCodeException.class, () -> parser.parseMessage(null));
    }

    @Test
    void testParseMessageWithEmptyInput() {
        assertThrows(ClaudeCodeException.class, () -> parser.parseMessage(""));
    }

    @Test
    void testParseMessageWithInvalidJson() {
        String invalidJson = "not json";

        assertThrows(ClaudeCodeException.class, () -> parser.parseMessage(invalidJson));
    }

    @Test
    void testParseMessagesWithValidArray() throws ClaudeCodeException {
        String jsonArray = "[{\"id\":\"1\",\"type\":\"TEXT\",\"content\":\"message 1\",\"metadata\":{},\"timestamp\":\"2024-01-01T00:00:00Z\"},{\"id\":\"2\",\"type\":\"TEXT\",\"content\":\"message 2\",\"metadata\":{},\"timestamp\":\"2024-01-01T00:00:00Z\"}]";

        List<Message> messages = parser.parseMessages(jsonArray);

        assertEquals(2, messages.size());
        assertEquals("1", messages.get(0).getId());
        assertEquals("2", messages.get(1).getId());
    }

    @Test
    void testParseMessagesWithSingleMessage() throws ClaudeCodeException {
        String json = "{\"id\":\"test-id\",\"type\":\"TEXT\",\"content\":\"test content\",\"metadata\":{},\"timestamp\":\"2024-01-01T00:00:00Z\"}";

        List<Message> messages = parser.parseMessages(json);

        assertEquals(1, messages.size());
        assertEquals("test-id", messages.get(0).getId());
    }

    @Test
    void testParseMessagesWithEmptyInput() throws ClaudeCodeException {
        List<Message> messages = parser.parseMessages("");

        assertTrue(messages.isEmpty());
    }

    @Test
    void testParseMessagesWithNullInput() throws ClaudeCodeException {
        List<Message> messages = parser.parseMessages(null);

        assertTrue(messages.isEmpty());
    }

    @Test
    void testParseStreamingMessages() {
        String streamContent = """
                data: {"id":"1","type":"TEXT","content":"message 1","metadata":{},"timestamp":"2024-01-01T00:00:00Z"}
                data: {"id":"2","type":"TEXT","content":"message 2","metadata":{},"timestamp":"2024-01-01T00:00:00Z"}
                // comment
                # another comment

                invalid line
                """;

        Stream<Message> messages = parser.parseStreamingMessages(streamContent);
        List<Message> messageList = messages.toList();

        assertEquals(2, messageList.size());
        assertEquals("1", messageList.get(0).getId());
        assertEquals("2", messageList.get(1).getId());
    }

    @Test
    void testIsValidJsonWithValidJson() {
        String validJson = "{\"key\":\"value\"}";

        assertTrue(parser.isValidJson(validJson));
    }

    @Test
    void testIsValidJsonWithInvalidJson() {
        String invalidJson = "{invalid}";

        assertFalse(parser.isValidJson(invalidJson));
    }

    @Test
    void testIsValidJsonWithNullInput() {
        assertFalse(parser.isValidJson(null));
    }

    @Test
    void testIsValidJsonWithEmptyInput() {
        assertFalse(parser.isValidJson(""));
    }

    @Test
    void testParseMetadata() throws ClaudeCodeException {
        String json = "{\"string\":\"value\",\"number\":42,\"boolean\":true,\"array\":[1,2,3],\"object\":{\"nested\":\"value\"}}";

        Map<String, Object> metadata = parser.parseMetadata(json);

        assertEquals("value", metadata.get("string"));
        assertEquals(42, metadata.get("number"));
        assertEquals(true, metadata.get("boolean"));
        assertInstanceOf(List.class, metadata.get("array"));
        assertInstanceOf(Map.class, metadata.get("object"));
    }

    @Test
    void testParseMetadataWithEmptyInput() throws ClaudeCodeException {
        Map<String, Object> metadata = parser.parseMetadata("");

        assertTrue(metadata.isEmpty());
    }

    @Test
    void testParseMetadataWithNullInput() throws ClaudeCodeException {
        Map<String, Object> metadata = parser.parseMetadata(null);

        assertTrue(metadata.isEmpty());
    }

    @Test
    void testParseMetadataWithInvalidJson() {
        String invalidJson = "{invalid}";

        assertThrows(ClaudeCodeException.class, () -> parser.parseMetadata(invalidJson));
    }

    @Test
    void testToJsonString() throws ClaudeCodeException {
        Map<String, Object> object = new HashMap<>();
        object.put("key", "value");
        object.put("number", 42);

        String json = parser.toJsonString(object);

        assertNotNull(json);
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
        assertTrue(json.contains("42"));
    }

    @Test
    void testValidateMessageFormat() {
        String validFormat = "{\"type\":\"TEXT\",\"content\":\"test\"}";
        String invalidFormat = "{\"missing\":\"type\"}";

        assertTrue(parser.validateMessageFormat(validFormat));
        assertFalse(parser.validateMessageFormat(invalidFormat));
    }

    @Test
    void testValidateMessageFormatWithInvalidJson() {
        String invalidJson = "{invalid}";

        assertFalse(parser.validateMessageFormat(invalidJson));
    }
}