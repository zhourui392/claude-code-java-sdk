package com.anthropic.claude.context;

import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文分析器
 * 负责分析上下文内容，提供智能排序和优先级管理
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ContextAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ContextAnalyzer.class);

    private final ContextConfig config;
    private final Map<String, Double> keywordWeights = new HashMap<>();

    public ContextAnalyzer(ContextConfig config) {
        this.config = config;
        initializeKeywordWeights();
        logger.debug("上下文分析器已初始化");
    }

    /**
     * 对消息按重要性排序
     *
     * @param messages 原始消息列表
     * @return 按重要性排序的消息列表
     */
    public List<Message> prioritizeMessages(List<Message> messages) {
        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        logger.debug("开始分析消息重要性，消息数量: {}", messages.size());

        List<MessageWithScore> scoredMessages = messages.stream()
                .map(msg -> new MessageWithScore(msg, calculateImportanceScore(msg)))
                .sorted(Comparator.comparingDouble(MessageWithScore::getScore).reversed())
                .collect(Collectors.toList());

        List<Message> prioritizedMessages = scoredMessages.stream()
                .map(MessageWithScore::getMessage)
                .collect(Collectors.toList());

        logger.debug("消息重要性分析完成，最高分: {:.2f}, 最低分: {:.2f}",
                scoredMessages.get(0).getScore(),
                scoredMessages.get(scoredMessages.size() - 1).getScore());

        return prioritizedMessages;
    }

    /**
     * 分析上下文模式
     *
     * @param messages 消息列表
     * @return 上下文模式分析结果
     */
    public ContextPattern analyzePattern(List<Message> messages) {
        if (messages.isEmpty()) {
            return new ContextPattern();
        }

        Map<MessageType, Integer> typeDistribution = new HashMap<>();
        Map<String, Integer> topicFrequency = new HashMap<>();
        double averageMessageLength = 0;
        LocalDateTime firstMessage = null;
        LocalDateTime lastMessage = null;

        for (Message message : messages) {
            // 统计消息类型分布
            typeDistribution.merge(message.getType(), 1, Integer::sum);

            // 计算平均长度
            averageMessageLength += message.getContent().length();

            // 提取主题关键词
            extractTopics(message.getContent()).forEach(topic ->
                    topicFrequency.merge(topic, 1, Integer::sum));

            // 时间范围统计
            LocalDateTime msgTime = message.getTimestamp();
            if (firstMessage == null || msgTime.isBefore(firstMessage)) {
                firstMessage = msgTime;
            }
            if (lastMessage == null || msgTime.isAfter(lastMessage)) {
                lastMessage = msgTime;
            }
        }

        averageMessageLength /= messages.size();

        return new ContextPattern(
                typeDistribution,
                topicFrequency,
                averageMessageLength,
                firstMessage,
                lastMessage,
                messages.size()
        );
    }

    /**
     * 检测上下文中的关键主题
     *
     * @param messages 消息列表
     * @return 主题列表，按重要性排序
     */
    public List<String> detectKeyTopics(List<Message> messages) {
        Map<String, Double> topicScores = new HashMap<>();

        for (Message message : messages) {
            double messageImportance = calculateImportanceScore(message);
            Set<String> topics = extractTopics(message.getContent());

            for (String topic : topics) {
                topicScores.merge(topic, messageImportance, Double::sum);
            }
        }

        return topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(10) // 返回前10个主题
                .collect(Collectors.toList());
    }

    /**
     * 检查上下文健康状态
     *
     * @param messages 消息列表
     * @return 健康状态评估
     */
    public ContextHealth assessHealth(List<Message> messages) {
        if (messages.isEmpty()) {
            return new ContextHealth(1.0, "上下文为空", Collections.emptyList());
        }

        List<String> issues = new ArrayList<>();
        double healthScore = 1.0;

        // 检查消息分布
        Map<MessageType, Integer> typeCount = messages.stream()
                .collect(Collectors.groupingBy(
                        Message::getType,
                        Collectors.reducing(0, msg -> 1, Integer::sum)
                ));

        // 检查是否有过多的错误消息
        int errorMessages = typeCount.getOrDefault(MessageType.ERROR, 0);
        if (errorMessages > messages.size() * 0.3) {
            healthScore -= 0.3;
            issues.add("错误消息过多: " + errorMessages);
        }

        // 检查消息时间分布
        if (messages.size() > 1) {
            LocalDateTime firstTime = messages.get(0).getTimestamp();
            LocalDateTime lastTime = messages.get(messages.size() - 1).getTimestamp();
            long timeSpanMinutes = ChronoUnit.MINUTES.between(firstTime, lastTime);

            if (timeSpanMinutes > config.getMaxContextAge()) {
                healthScore -= 0.2;
                issues.add("上下文时间跨度过长: " + timeSpanMinutes + " 分钟");
            }
        }

        // 检查平均消息长度
        double avgLength = messages.stream()
                .mapToInt(msg -> msg.getContent().length())
                .average()
                .orElse(0.0);

        if (avgLength < 10) {
            healthScore -= 0.1;
            issues.add("平均消息长度过短: " + avgLength);
        }

        // 检查消息数量
        if (messages.size() > config.getMaxWindowSize() / 50) {
            healthScore -= 0.2;
            issues.add("消息数量过多: " + messages.size());
        }

        healthScore = Math.max(0.0, healthScore);

        String status = healthScore > 0.8 ? "健康" :
                       healthScore > 0.6 ? "良好" :
                       healthScore > 0.4 ? "一般" : "需要优化";

        return new ContextHealth(healthScore, status, issues);
    }

    /**
     * 计算消息重要性分数
     */
    private double calculateImportanceScore(Message message) {
        double score = 0.0;

        // 基础分数根据消息类型
        score += getTypeScore(message.getType());

        // 基于内容的分数
        score += calculateContentScore(message.getContent());

        // 基于时间的分数
        score += calculateTimeScore(message.getTimestamp());

        // 基于长度的分数
        score += calculateLengthScore(message.getContent().length());

        return Math.min(1.0, score);
    }

    /**
     * 获取消息类型分数
     */
    private double getTypeScore(MessageType type) {
        switch (type) {
            case SYSTEM:
                return 0.8;
            case ERROR:
                return 0.7;
            case USER:
                return 0.5;
            case ASSISTANT:
                return 0.4;
            default:
                return 0.3;
        }
    }

    /**
     * 计算内容分数
     */
    private double calculateContentScore(String content) {
        double score = 0.0;
        String lowerContent = content.toLowerCase();

        for (Map.Entry<String, Double> entry : keywordWeights.entrySet()) {
            if (lowerContent.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }

        return Math.min(0.4, score); // 最多贡献0.4分
    }

    /**
     * 计算时间分数（越新分数越高）
     */
    private double calculateTimeScore(LocalDateTime timestamp) {
        long minutesAgo = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());

        if (minutesAgo < 5) {
            return 0.2;
        } else if (minutesAgo < 30) {
            return 0.15;
        } else if (minutesAgo < 60) {
            return 0.1;
        } else {
            return 0.0;
        }
    }

    /**
     * 计算长度分数
     */
    private double calculateLengthScore(int length) {
        if (length < 50) {
            return 0.05;
        } else if (length < 200) {
            return 0.1;
        } else if (length < 500) {
            return 0.15;
        } else {
            return 0.2;
        }
    }

    /**
     * 提取主题关键词
     */
    private Set<String> extractTopics(String content) {
        Set<String> topics = new HashSet<>();
        String lowerContent = content.toLowerCase();

        // 简单的关键词提取
        for (String keyword : keywordWeights.keySet()) {
            if (lowerContent.contains(keyword)) {
                topics.add(keyword);
            }
        }

        return topics;
    }

    /**
     * 初始化关键词权重
     */
    private void initializeKeywordWeights() {
        // 错误相关
        keywordWeights.put("error", 0.3);
        keywordWeights.put("exception", 0.3);
        keywordWeights.put("failed", 0.2);
        keywordWeights.put("错误", 0.3);

        // 重要性关键词
        keywordWeights.put("important", 0.2);
        keywordWeights.put("critical", 0.3);
        keywordWeights.put("urgent", 0.25);
        keywordWeights.put("重要", 0.2);
        keywordWeights.put("关键", 0.3);

        // 技术关键词
        keywordWeights.put("bug", 0.2);
        keywordWeights.put("issue", 0.15);
        keywordWeights.put("problem", 0.15);
        keywordWeights.put("solution", 0.1);
        keywordWeights.put("fix", 0.1);

        // 操作关键词
        keywordWeights.put("create", 0.1);
        keywordWeights.put("delete", 0.15);
        keywordWeights.put("update", 0.1);
        keywordWeights.put("install", 0.1);
    }

    /**
     * 带分数的消息包装类
     */
    private static class MessageWithScore {
        private final Message message;
        private final double score;

        public MessageWithScore(Message message, double score) {
            this.message = message;
            this.score = score;
        }

        public Message getMessage() { return message; }
        public double getScore() { return score; }
    }

    /**
     * 上下文模式分析结果
     */
    public static class ContextPattern {
        private final Map<MessageType, Integer> typeDistribution;
        private final Map<String, Integer> topicFrequency;
        private final double averageMessageLength;
        private final LocalDateTime firstMessage;
        private final LocalDateTime lastMessage;
        private final int totalMessages;

        public ContextPattern() {
            this(new HashMap<>(), new HashMap<>(), 0.0, null, null, 0);
        }

        public ContextPattern(Map<MessageType, Integer> typeDistribution,
                             Map<String, Integer> topicFrequency,
                             double averageMessageLength,
                             LocalDateTime firstMessage,
                             LocalDateTime lastMessage,
                             int totalMessages) {
            this.typeDistribution = new HashMap<>(typeDistribution);
            this.topicFrequency = new HashMap<>(topicFrequency);
            this.averageMessageLength = averageMessageLength;
            this.firstMessage = firstMessage;
            this.lastMessage = lastMessage;
            this.totalMessages = totalMessages;
        }

        // Getters
        public Map<MessageType, Integer> getTypeDistribution() { return new HashMap<>(typeDistribution); }
        public Map<String, Integer> getTopicFrequency() { return new HashMap<>(topicFrequency); }
        public double getAverageMessageLength() { return averageMessageLength; }
        public LocalDateTime getFirstMessage() { return firstMessage; }
        public LocalDateTime getLastMessage() { return lastMessage; }
        public int getTotalMessages() { return totalMessages; }

        public long getTimeSpanMinutes() {
            if (firstMessage == null || lastMessage == null) return 0;
            return ChronoUnit.MINUTES.between(firstMessage, lastMessage);
        }
    }

    /**
     * 上下文健康状态
     */
    public static class ContextHealth {
        private final double score;
        private final String status;
        private final List<String> issues;

        public ContextHealth(double score, String status, List<String> issues) {
            this.score = score;
            this.status = status;
            this.issues = new ArrayList<>(issues);
        }

        public double getScore() { return score; }
        public String getStatus() { return status; }
        public List<String> getIssues() { return new ArrayList<>(issues); }

        public boolean isHealthy() { return score > 0.8; }

        @Override
        public String toString() {
            return String.format("ContextHealth{score=%.2f, status='%s', issues=%d}",
                    score, status, issues.size());
        }
    }
}