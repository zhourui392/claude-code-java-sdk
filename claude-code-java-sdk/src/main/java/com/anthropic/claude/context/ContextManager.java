package com.anthropic.claude.context;

import com.anthropic.claude.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 上下文管理器
 * 负责管理对话上下文，包括自动压缩、大小监控和智能截断
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ContextManager {
    private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);

    private final ContextConfig config;
    private final Map<String, ContextWindow> contexts = new ConcurrentHashMap<>();
    private final ContextCompressor compressor;
    private final ContextAnalyzer analyzer;
    private final AtomicLong totalContextSize = new AtomicLong(0);

    public ContextManager(ContextConfig config) {
        this.config = config;
        this.compressor = new ContextCompressor(config);
        this.analyzer = new ContextAnalyzer(config);
        logger.info("上下文管理器已初始化 - 最大窗口大小: {} tokens", config.getMaxWindowSize());
    }

    /**
     * 创建新的上下文窗口
     *
     * @param contextId 上下文ID
     * @return 上下文窗口
     */
    public ContextWindow createContext(String contextId) {
        ContextWindow window = new ContextWindow(contextId, config);
        contexts.put(contextId, window);
        logger.debug("创建上下文窗口: {}", contextId);
        return window;
    }

    /**
     * 获取上下文窗口
     *
     * @param contextId 上下文ID
     * @return 上下文窗口，不存在时返回null
     */
    public ContextWindow getContext(String contextId) {
        return contexts.get(contextId);
    }

    /**
     * 添加消息到上下文
     *
     * @param contextId 上下文ID
     * @param message 消息
     */
    public void addMessage(String contextId, Message message) {
        ContextWindow window = contexts.get(contextId);
        if (window == null) {
            window = createContext(contextId);
        }

        // 添加消息前检查是否需要压缩
        if (shouldCompress(window)) {
            compressContext(window);
        }

        window.addMessage(message);
        updateTotalSize();

        logger.debug("添加消息到上下文 {} - 当前大小: {} tokens",
                contextId, window.getCurrentSize());
    }

    /**
     * 获取上下文消息列表
     *
     * @param contextId 上下文ID
     * @return 消息列表
     */
    public List<Message> getMessages(String contextId) {
        ContextWindow window = contexts.get(contextId);
        return window != null ? window.getMessages() : Collections.emptyList();
    }

    /**
     * 获取压缩后的上下文
     *
     * @param contextId 上下文ID
     * @return 压缩后的消息列表
     */
    public List<Message> getCompressedContext(String contextId) {
        ContextWindow window = contexts.get(contextId);
        if (window == null) {
            return Collections.emptyList();
        }

        return compressor.compress(window.getMessages());
    }

    /**
     * 清理上下文
     *
     * @param contextId 上下文ID
     * @return 是否成功清理
     */
    public boolean clearContext(String contextId) {
        ContextWindow window = contexts.remove(contextId);
        if (window != null) {
            updateTotalSize();
            logger.info("清理上下文: {}", contextId);
            return true;
        }
        return false;
    }

    /**
     * 获取上下文统计信息
     *
     * @return 统计信息
     */
    public ContextStats getStats() {
        int totalContexts = contexts.size();
        long totalSize = totalContextSize.get();
        long averageSize = totalContexts > 0 ? totalSize / totalContexts : 0;

        Map<String, Long> contextSizes = contexts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (long) entry.getValue().getCurrentSize()
                ));

        return new ContextStats(totalContexts, totalSize, averageSize, contextSizes);
    }

    /**
     * 压缩指定上下文
     *
     * @param contextId 上下文ID
     * @return 压缩前后的大小差异
     */
    public int compressContext(String contextId) {
        ContextWindow window = contexts.get(contextId);
        if (window == null) {
            return 0;
        }

        return compressContext(window);
    }

    /**
     * 压缩所有上下文
     *
     * @return 总压缩大小
     */
    public long compressAllContexts() {
        long totalCompressed = 0;
        for (ContextWindow window : contexts.values()) {
            if (shouldCompress(window)) {
                totalCompressed += compressContext(window);
            }
        }
        logger.info("批量压缩完成，节省空间: {} tokens", totalCompressed);
        return totalCompressed;
    }

    /**
     * 获取上下文优先级排序
     *
     * @param contextId 上下文ID
     * @return 按重要性排序的消息列表
     */
    public List<Message> getPrioritizedMessages(String contextId) {
        ContextWindow window = contexts.get(contextId);
        if (window == null) {
            return Collections.emptyList();
        }

        return analyzer.prioritizeMessages(window.getMessages());
    }

    /**
     * 检查是否需要压缩
     */
    private boolean shouldCompress(ContextWindow window) {
        return window.getCurrentSize() > config.getCompressionThreshold();
    }

    /**
     * 执行上下文压缩
     */
    private int compressContext(ContextWindow window) {
        int originalSize = window.getCurrentSize();

        List<Message> compressedMessages = compressor.compress(window.getMessages());
        window.setMessages(compressedMessages);

        int newSize = window.getCurrentSize();
        int saved = originalSize - newSize;

        logger.info("压缩上下文 {} - 原始: {} tokens, 压缩后: {} tokens, 节省: {} tokens",
                window.getContextId(), originalSize, newSize, saved);

        updateTotalSize();
        return saved;
    }

    /**
     * 更新总大小统计
     */
    private void updateTotalSize() {
        long total = contexts.values().stream()
                .mapToLong(window -> window.getCurrentSize())
                .sum();
        totalContextSize.set(total);
    }

    /**
     * 关闭上下文管理器
     */
    public void shutdown() {
        logger.info("正在关闭上下文管理器...");
        contexts.clear();
        totalContextSize.set(0);
        logger.info("上下文管理器已关闭");
    }

    /**
     * 上下文统计信息
     */
    public static class ContextStats {
        private final int totalContexts;
        private final long totalSize;
        private final long averageSize;
        private final Map<String, Long> contextSizes;

        public ContextStats(int totalContexts, long totalSize, long averageSize,
                           Map<String, Long> contextSizes) {
            this.totalContexts = totalContexts;
            this.totalSize = totalSize;
            this.averageSize = averageSize;
            this.contextSizes = new HashMap<>(contextSizes);
        }

        public int getTotalContexts() { return totalContexts; }
        public long getTotalSize() { return totalSize; }
        public long getAverageSize() { return averageSize; }
        public Map<String, Long> getContextSizes() { return new HashMap<>(contextSizes); }

        @Override
        public String toString() {
            return String.format("ContextStats{contexts=%d, totalSize=%d, avgSize=%d}",
                    totalContexts, totalSize, averageSize);
        }
    }

    /**
     * 上下文窗口内部类
     */
    public static class ContextWindow {
        private final String contextId;
        private final ContextConfig config;
        private final LocalDateTime createdAt;
        private volatile List<Message> messages;
        private volatile int currentSize;
        private volatile LocalDateTime lastAccess;

        public ContextWindow(String contextId, ContextConfig config) {
            this.contextId = contextId;
            this.config = config;
            this.createdAt = LocalDateTime.now();
            this.messages = new ArrayList<>();
            this.currentSize = 0;
            this.lastAccess = LocalDateTime.now();
        }

        public void addMessage(Message message) {
            messages.add(message);
            currentSize += estimateTokenCount(message);
            lastAccess = LocalDateTime.now();

            // 检查是否超过最大窗口大小
            if (currentSize > config.getMaxWindowSize()) {
                truncateToFit();
            }
        }

        public List<Message> getMessages() {
            lastAccess = LocalDateTime.now();
            return new ArrayList<>(messages);
        }

        public void setMessages(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
            this.currentSize = messages.stream()
                    .mapToInt(this::estimateTokenCount)
                    .sum();
            this.lastAccess = LocalDateTime.now();
        }

        public String getContextId() { return contextId; }
        public int getCurrentSize() { return currentSize; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccess() { return lastAccess; }

        /**
         * 截断消息以适应窗口大小
         */
        private void truncateToFit() {
            int targetSize = (int) (config.getMaxWindowSize() * 0.8); // 保留80%空间
            List<Message> newMessages = new ArrayList<>();
            int totalSize = 0;

            // 从最新消息开始保留
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                int messageSize = estimateTokenCount(message);

                if (totalSize + messageSize <= targetSize) {
                    newMessages.add(0, message);
                    totalSize += messageSize;
                } else {
                    break;
                }
            }

            this.messages = newMessages;
            this.currentSize = totalSize;
        }

        /**
         * 估算消息token数量
         */
        private int estimateTokenCount(Message message) {
            // 简单估算：平均每4个字符为1个token
            return Math.max(1, message.getContent().length() / 4);
        }
    }
}