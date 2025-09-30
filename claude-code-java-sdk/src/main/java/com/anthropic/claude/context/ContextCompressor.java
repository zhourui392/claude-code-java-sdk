package com.anthropic.claude.context;

import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文压缩器
 * 负责智能压缩对话上下文，保留重要信息
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ContextCompressor {
    private static final Logger logger = LoggerFactory.getLogger(ContextCompressor.class);

    private final ContextConfig config;
    private final MessageImportanceCalculator importanceCalculator;

    public ContextCompressor(ContextConfig config) {
        this.config = config;
        this.importanceCalculator = new MessageImportanceCalculator();
        logger.debug("上下文压缩器已初始化");
    }

    /**
     * 压缩消息列表
     *
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    public List<Message> compress(List<Message> messages) {
        if (messages.isEmpty()) {
            return new ArrayList<>(messages);
        }

        logger.debug("开始压缩上下文，原始消息数: {}", messages.size());

        // 1. 计算目标大小
        int currentSize = estimateTotalSize(messages);
        int targetSize = (int) (currentSize * config.getCompressionRatio());

        // 2. 分类消息
        List<Message> importantMessages = new ArrayList<>();
        List<Message> regularMessages = new ArrayList<>();
        List<Message> systemMessages = new ArrayList<>();

        classifyMessages(messages, importantMessages, regularMessages, systemMessages);

        // 3. 构建压缩结果
        List<Message> compressedMessages = new ArrayList<>();

        // 始终保留系统消息
        compressedMessages.addAll(systemMessages);

        // 保留重要消息（如果配置了）
        if (config.isPreserveImportantMessages()) {
            compressedMessages.addAll(importantMessages);
        }

        // 从最新的常规消息开始选择
        List<Message> selectedRegular = selectRecentMessages(regularMessages, targetSize,
                estimateTotalSize(compressedMessages));

        compressedMessages.addAll(selectedRegular);

        // 4. 按时间顺序排序
        compressedMessages.sort(Comparator.comparing(Message::getTimestamp));

        // 5. 确保满足最小消息数要求
        ensureMinimumMessages(compressedMessages, messages);

        logger.debug("压缩完成 - 原始: {} 消息, 压缩后: {} 消息",
                messages.size(), compressedMessages.size());

        return compressedMessages;
    }

    /**
     * 智能截断消息以适应大小限制
     *
     * @param messages 消息列表
     * @param maxSize 最大大小
     * @return 截断后的消息列表
     */
    public List<Message> truncateToSize(List<Message> messages, int maxSize) {
        if (estimateTotalSize(messages) <= maxSize) {
            return new ArrayList<>(messages);
        }

        logger.debug("截断消息以适应大小限制: {} tokens", maxSize);

        List<Message> result = new ArrayList<>();
        int currentSize = 0;

        // 从最新消息开始保留
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            int messageSize = estimateMessageSize(message);

            if (currentSize + messageSize <= maxSize) {
                result.add(0, message);
                currentSize += messageSize;
            } else {
                // 如果是重要消息，尝试压缩内容
                if (isImportantMessage(message)) {
                    Message compressedMessage = compressMessage(message, maxSize - currentSize);
                    if (compressedMessage != null) {
                        result.add(0, compressedMessage);
                        currentSize += estimateMessageSize(compressedMessage);
                    }
                }
                break;
            }
        }

        logger.debug("截断完成 - 保留: {} 消息, 大小: {} tokens", result.size(), currentSize);
        return result;
    }

    /**
     * 分类消息
     */
    private void classifyMessages(List<Message> messages,
                                List<Message> importantMessages,
                                List<Message> regularMessages,
                                List<Message> systemMessages) {
        for (Message message : messages) {
            if (message.getType() == MessageType.SYSTEM) {
                systemMessages.add(message);
            } else if (isImportantMessage(message)) {
                importantMessages.add(message);
            } else {
                regularMessages.add(message);
            }
        }

        logger.debug("消息分类完成 - 重要: {}, 常规: {}, 系统: {}",
                importantMessages.size(), regularMessages.size(), systemMessages.size());
    }

    /**
     * 选择最近的消息
     */
    private List<Message> selectRecentMessages(List<Message> messages, int targetSize, int usedSize) {
        List<Message> selected = new ArrayList<>();
        int remainingSize = targetSize - usedSize;

        if (remainingSize <= 0) {
            return selected;
        }

        // 按时间倒序排序（最新的优先）
        List<Message> sortedMessages = messages.stream()
                .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                .collect(Collectors.toList());

        int currentSize = 0;
        for (Message message : sortedMessages) {
            int messageSize = estimateMessageSize(message);
            if (currentSize + messageSize <= remainingSize) {
                selected.add(message);
                currentSize += messageSize;
            }
        }

        return selected;
    }

    /**
     * 确保满足最小消息数要求
     */
    private void ensureMinimumMessages(List<Message> compressedMessages, List<Message> originalMessages) {
        if (compressedMessages.size() >= config.getMinRetainedMessages()) {
            return;
        }

        int needed = config.getMinRetainedMessages() - compressedMessages.size();
        Set<Message> existing = new HashSet<>(compressedMessages);

        // 从原消息中选择最新的消息补充
        List<Message> candidates = originalMessages.stream()
                .filter(msg -> !existing.contains(msg))
                .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                .limit(needed)
                .collect(Collectors.toList());

        compressedMessages.addAll(candidates);
        compressedMessages.sort(Comparator.comparing(Message::getTimestamp));

        logger.debug("补充消息以满足最小数量要求: {}", needed);
    }

    /**
     * 判断是否为重要消息
     */
    private boolean isImportantMessage(Message message) {
        return importanceCalculator.calculateImportance(message) > 0.7; // 重要性阈值
    }

    /**
     * 压缩单个消息
     */
    private Message compressMessage(Message message, int maxSize) {
        String content = message.getContent();
        if (content.length() <= maxSize * 4) { // 粗略估算
            return message;
        }

        // 简单截断策略：保留开头和结尾
        int keepSize = maxSize * 2; // 保留字符数
        if (content.length() <= keepSize) {
            return message;
        }

        int halfSize = keepSize / 2;
        String compressed = content.substring(0, halfSize) +
                           "\n[... 内容已压缩 ...]\n" +
                           content.substring(content.length() - halfSize);

        return new Message(message.getType(), compressed, message.getTimestamp());
    }

    /**
     * 估算消息大小（token数）
     */
    private int estimateMessageSize(Message message) {
        return Math.max(1, message.getContent().length() / 4);
    }

    /**
     * 估算消息列表总大小
     */
    private int estimateTotalSize(List<Message> messages) {
        return messages.stream()
                .mapToInt(this::estimateMessageSize)
                .sum();
    }

    /**
     * 消息重要性计算器
     */
    private static class MessageImportanceCalculator {

        /**
         * 计算消息重要性分数 (0.0 - 1.0)
         */
        public double calculateImportance(Message message) {
            double score = 0.0;

            // 系统消息通常很重要
            if (message.getType() == MessageType.SYSTEM) {
                score += 0.8;
            }

            String content = message.getContent().toLowerCase();

            // 包含错误信息的消息较重要
            if (content.contains("error") || content.contains("exception") ||
                content.contains("failed") || content.contains("错误")) {
                score += 0.3;
            }

            // 包含重要关键词的消息
            if (content.contains("important") || content.contains("重要") ||
                content.contains("critical") || content.contains("关键")) {
                score += 0.2;
            }

            // 较长的消息通常包含更多信息
            if (message.getContent().length() > 500) {
                score += 0.1;
            }

            // 最近的消息相对重要
            long ageMinutes = java.time.Duration.between(
                    message.getTimestamp(),
                    java.time.LocalDateTime.now()
            ).toMinutes();

            if (ageMinutes < 10) {
                score += 0.2;
            } else if (ageMinutes < 30) {
                score += 0.1;
            }

            return Math.min(1.0, score);
        }
    }
}