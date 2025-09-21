package com.anthropic.claude.messages;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MessageParser {
    private static final Logger logger = LoggerFactory.getLogger(MessageParser.class);

    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;

    public MessageParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.jsonFactory = new JsonFactory();
    }

    public Message parseMessage(String jsonString) throws ClaudeCodeException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new ClaudeCodeException("JSON字符串为空");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 处理Claude CLI的响应格式
            if (jsonNode.has("result") && jsonNode.has("type") &&
                "result".equals(jsonNode.get("type").asText())) {

                String content = jsonNode.get("result").asText();
                String subtype = jsonNode.has("subtype") ? jsonNode.get("subtype").asText() : null;
                String id = jsonNode.has("uuid") ? jsonNode.get("uuid").asText() : null;

                // 创建标准Message对象
                return new Message(id, "TEXT", subtype, content, null, null);
            }

            // 尝试直接解析为Message格式
            return objectMapper.readValue(jsonString, Message.class);
        } catch (IOException e) {
            logger.error("解析消息失败: {}", jsonString, e);
            throw new ClaudeCodeException("MESSAGE_PARSE_ERROR", "解析消息失败", e);
        }
    }

    public List<Message> parseMessages(String jsonArrayString) throws ClaudeCodeException {
        if (jsonArrayString == null || jsonArrayString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JsonNode arrayNode = objectMapper.readTree(jsonArrayString);
            List<Message> messages = new ArrayList<>();

            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    String messageJson = objectMapper.writeValueAsString(node);
                    messages.add(parseMessage(messageJson));
                }
            } else {
                messages.add(parseMessage(jsonArrayString));
            }

            return messages;
        } catch (IOException e) {
            logger.error("解析消息数组失败: {}", jsonArrayString, e);
            throw new ClaudeCodeException("MESSAGE_ARRAY_PARSE_ERROR", "解析消息数组失败", e);
        }
    }

    public Stream<Message> parseStreamingMessages(String streamContent) {
        List<Message> messages = new ArrayList<>();

        try {
            String[] lines = streamContent.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("data: ")) {
                    line = line.substring(6).trim();
                }

                if (isValidJson(line)) {
                    try {
                        Message message = parseMessage(line);
                        messages.add(message);
                    } catch (ClaudeCodeException e) {
                        logger.warn("跳过无法解析的消息行: {}", line);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析流式消息时出错", e);
        }

        return messages.stream();
    }

    public boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }

        try (JsonParser parser = jsonFactory.createParser(new StringReader(jsonString))) {
            while (parser.nextToken() != null) {
                // 遍历所有token检查格式
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Map<String, Object> parseMetadata(String jsonString) throws ClaudeCodeException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            JsonNode node = objectMapper.readTree(jsonString);
            Map<String, Object> metadata = new HashMap<>();

            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                metadata.put(key, convertJsonNodeToObject(value));
            });

            return metadata;
        } catch (IOException e) {
            logger.error("解析元数据失败: {}", jsonString, e);
            throw new ClaudeCodeException("METADATA_PARSE_ERROR", "解析元数据失败", e);
        }
    }

    private Object convertJsonNodeToObject(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isInt()) {
            return node.intValue();
        } else if (node.isLong()) {
            return node.longValue();
        } else if (node.isDouble()) {
            return node.doubleValue();
        } else if (node.isTextual()) {
            return node.textValue();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode element : node) {
                list.add(convertJsonNodeToObject(element));
            }
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), convertJsonNodeToObject(entry.getValue()));
            });
            return map;
        }
        return node.toString();
    }

    public String toJsonString(Object object) throws ClaudeCodeException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            logger.error("对象序列化为JSON失败", e);
            throw new ClaudeCodeException("JSON_SERIALIZE_ERROR", "对象序列化为JSON失败", e);
        }
    }

    public boolean validateMessageFormat(String jsonString) {
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            return node.has("type") && node.has("content");
        } catch (IOException e) {
            return false;
        }
    }
}