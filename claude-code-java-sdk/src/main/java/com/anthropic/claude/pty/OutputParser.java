package com.anthropic.claude.pty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Claude CLI输出解析器
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class OutputParser {
    private static final Logger logger = LoggerFactory.getLogger(OutputParser.class);

    private final StringBuilder buffer = new StringBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ClaudeState currentState = ClaudeState.STARTING;
    private ClaudeResponseBuilder responseBuilder = new ClaudeResponseBuilder();

    /**
     * 解析CLI输出并返回状态变化
     *
     * @param output 输出内容
     * @return 状态变化列表
     */
    public List<StateChange> parseOutput(String output) {
        List<StateChange> changes = new ArrayList<>();
        buffer.append(output);

        String[] lines = buffer.toString().split("\n");

        // 保留最后一行（可能不完整）
        if (lines.length > 0) {
            String lastLine = lines[lines.length - 1];
            buffer.setLength(0);
            if (!output.endsWith("\n")) {
                buffer.append(lastLine);
            }

            // 处理完整的行
            for (int i = 0; i < lines.length - (output.endsWith("\n") ? 0 : 1); i++) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    changes.addAll(parseLine(line));
                }
            }
        }

        return changes;
    }

    /**
     * 解析单行输出
     */
    private List<StateChange> parseLine(String line) {
        List<StateChange> changes = new ArrayList<>();
        ClaudeState previousState = currentState;

        try {
            // 按优先级检测各种模式
            if (detectPrompt(line)) {
                currentState = ClaudeState.READY;
                changes.add(new StateChange(previousState, currentState, line));
                responseBuilder.complete();

            } else if (detectError(line)) {
                currentState = ClaudeState.ERROR;
                ClaudeResponse errorResponse = new ClaudeResponse(
                    ClaudeResponse.ClaudeResponseType.ERROR_RESPONSE, line, currentState);
                changes.add(new StateChange(currentState, errorResponse));
                responseBuilder.setError(line);

            } else if (detectUsageLimit(line)) {
                currentState = ClaudeState.USAGE_LIMIT;
                ClaudeResponse limitResponse = new ClaudeResponse(
                    ClaudeResponse.ClaudeResponseType.STATUS_UPDATE, line, currentState);
                changes.add(new StateChange(currentState, limitResponse));

            } else if (detectSessionInfo(line)) {
                currentState = ClaudeState.SESSION_RESTORED;
                ClaudeResponse sessionResponse = parseSessionInfo(line);
                changes.add(new StateChange(currentState, sessionResponse));

            } else if (detectAuthRequired(line)) {
                currentState = ClaudeState.AUTH_REQUIRED;
                ClaudeResponse authResponse = new ClaudeResponse(
                    ClaudeResponse.ClaudeResponseType.STATUS_UPDATE, line, currentState);
                changes.add(new StateChange(currentState, authResponse));

            } else if (detectProcessing(line)) {
                if (currentState != ClaudeState.PROCESSING) {
                    currentState = ClaudeState.PROCESSING;
                    changes.add(new StateChange(previousState, currentState, line));
                }

            } else if (detectStreaming(line)) {
                if (currentState != ClaudeState.STREAMING) {
                    currentState = ClaudeState.STREAMING;
                    changes.add(new StateChange(previousState, currentState, line));
                }
                responseBuilder.appendContent(line);

            } else if (detectWaitingInput(line)) {
                currentState = ClaudeState.WAITING_INPUT;
                changes.add(new StateChange(previousState, currentState, line));

            } else if (detectInterrupt(line)) {
                currentState = ClaudeState.INTERRUPTED;
                changes.add(new StateChange(previousState, currentState, line));

            } else if (detectJsonResponse(line)) {
                // 尝试解析JSON响应
                ClaudeResponse jsonResponse = parseJsonResponse(line);
                if (jsonResponse != null) {
                    currentState = ClaudeState.COMPLETED;
                    changes.add(new StateChange(currentState, jsonResponse));
                } else {
                    // JSON解析失败，作为普通文本处理
                    responseBuilder.appendContent(line);
                }

            } else {
                // 普通文本内容
                responseBuilder.appendContent(line);
                if (currentState == ClaudeState.STARTING) {
                    currentState = ClaudeState.PROCESSING;
                    changes.add(new StateChange(previousState, currentState, line));
                }
            }

        } catch (Exception e) {
            logger.warn("解析输出行时出错: {}", line, e);
            // 出错时作为普通文本处理
            responseBuilder.appendContent(line);
        }

        return changes;
    }

    // 各种检测方法
    private boolean detectPrompt(String line) {
        return ClaudeOutputPatterns.PROMPT_PATTERN.matcher(line).find();
    }

    private boolean detectError(String line) {
        return ClaudeOutputPatterns.ERROR_PATTERN.matcher(line).find();
    }

    private boolean detectUsageLimit(String line) {
        return ClaudeOutputPatterns.USAGE_LIMIT_PATTERN.matcher(line).find();
    }

    private boolean detectSessionInfo(String line) {
        return ClaudeOutputPatterns.SESSION_RESTORED_PATTERN.matcher(line).find() ||
               ClaudeOutputPatterns.SESSION_ID_PATTERN.matcher(line).find();
    }

    private boolean detectAuthRequired(String line) {
        return ClaudeOutputPatterns.AUTH_REQUIRED_PATTERN.matcher(line).find();
    }

    private boolean detectProcessing(String line) {
        return ClaudeOutputPatterns.PROCESSING_PATTERN.matcher(line).find();
    }

    private boolean detectStreaming(String line) {
        return ClaudeOutputPatterns.STREAMING_START_PATTERN.matcher(line).find() ||
               currentState == ClaudeState.STREAMING;
    }

    private boolean detectWaitingInput(String line) {
        return ClaudeOutputPatterns.WAITING_INPUT_PATTERN.matcher(line).find();
    }

    private boolean detectInterrupt(String line) {
        return ClaudeOutputPatterns.INTERRUPT_PATTERN.matcher(line).find();
    }

    private boolean detectJsonResponse(String line) {
        return ClaudeOutputPatterns.JSON_RESPONSE_PATTERN.matcher(line).matches();
    }

    /**
     * 解析会话信息
     */
    private ClaudeResponse parseSessionInfo(String line) {
        ClaudeResponse response = new ClaudeResponse(
            ClaudeResponse.ClaudeResponseType.SESSION_INFO, line, ClaudeState.SESSION_RESTORED);

        // 提取会话ID
        Matcher sessionMatcher = ClaudeOutputPatterns.SESSION_ID_PATTERN.matcher(line);
        if (sessionMatcher.find()) {
            response.setSessionId(sessionMatcher.group(1));
            response.getMetadata().put("session_id", sessionMatcher.group(1));
        }

        return response;
    }

    /**
     * 解析JSON响应
     */
    private ClaudeResponse parseJsonResponse(String line) {
        try {
            JsonNode jsonNode = objectMapper.readTree(line);
            ClaudeResponse response = new ClaudeResponse(
                ClaudeResponse.ClaudeResponseType.JSON_RESPONSE, line, ClaudeState.COMPLETED);

            // 提取常见字段
            if (jsonNode.has("type")) {
                response.getMetadata().put("response_type", jsonNode.get("type").asText());
            }
            if (jsonNode.has("content")) {
                response.setContent(jsonNode.get("content").asText());
            }
            if (jsonNode.has("session_id")) {
                response.setSessionId(jsonNode.get("session_id").asText());
            }

            return response;
        } catch (Exception e) {
            logger.debug("JSON解析失败，作为普通文本处理: {}", line);
            return null;
        }
    }

    /**
     * 获取当前状态
     */
    public ClaudeState getCurrentState() {
        return currentState;
    }

    /**
     * 重置解析器状态
     */
    public void reset() {
        buffer.setLength(0);
        currentState = ClaudeState.STARTING;
        responseBuilder = new ClaudeResponseBuilder();
    }
}