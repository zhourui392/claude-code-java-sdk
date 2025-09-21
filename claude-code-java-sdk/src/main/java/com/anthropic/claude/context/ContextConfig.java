package com.anthropic.claude.context;

/**
 * 上下文配置
 * 定义上下文管理的各种参数
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ContextConfig {

    private final int maxWindowSize;
    private final int compressionThreshold;
    private final int minRetainedMessages;
    private final double compressionRatio;
    private final boolean enableSmartTruncation;
    private final boolean preserveImportantMessages;
    private final int maxContextAge; // 分钟

    private ContextConfig(Builder builder) {
        this.maxWindowSize = builder.maxWindowSize;
        this.compressionThreshold = builder.compressionThreshold;
        this.minRetainedMessages = builder.minRetainedMessages;
        this.compressionRatio = builder.compressionRatio;
        this.enableSmartTruncation = builder.enableSmartTruncation;
        this.preserveImportantMessages = builder.preserveImportantMessages;
        this.maxContextAge = builder.maxContextAge;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ContextConfig defaultConfig() {
        return builder().build();
    }

    // Getters
    public int getMaxWindowSize() { return maxWindowSize; }
    public int getCompressionThreshold() { return compressionThreshold; }
    public int getMinRetainedMessages() { return minRetainedMessages; }
    public double getCompressionRatio() { return compressionRatio; }
    public boolean isEnableSmartTruncation() { return enableSmartTruncation; }
    public boolean isPreserveImportantMessages() { return preserveImportantMessages; }
    public int getMaxContextAge() { return maxContextAge; }

    public static class Builder {
        private int maxWindowSize = 8000; // Claude-3的默认上下文窗口
        private int compressionThreshold = 6000; // 75%时开始压缩
        private int minRetainedMessages = 10; // 最少保留消息数
        private double compressionRatio = 0.6; // 压缩到原大小的60%
        private boolean enableSmartTruncation = true; // 启用智能截断
        private boolean preserveImportantMessages = true; // 保留重要消息
        private int maxContextAge = 60; // 60分钟后清理

        public Builder maxWindowSize(int maxWindowSize) {
            this.maxWindowSize = maxWindowSize;
            return this;
        }

        public Builder compressionThreshold(int compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
            return this;
        }

        public Builder minRetainedMessages(int minRetainedMessages) {
            this.minRetainedMessages = minRetainedMessages;
            return this;
        }

        public Builder compressionRatio(double compressionRatio) {
            this.compressionRatio = compressionRatio;
            return this;
        }

        public Builder enableSmartTruncation(boolean enableSmartTruncation) {
            this.enableSmartTruncation = enableSmartTruncation;
            return this;
        }

        public Builder preserveImportantMessages(boolean preserveImportantMessages) {
            this.preserveImportantMessages = preserveImportantMessages;
            return this;
        }

        public Builder maxContextAge(int maxContextAge) {
            this.maxContextAge = maxContextAge;
            return this;
        }

        public ContextConfig build() {
            validate();
            return new ContextConfig(this);
        }

        private void validate() {
            if (maxWindowSize <= 0) {
                throw new IllegalArgumentException("最大窗口大小必须大于0");
            }
            if (compressionThreshold <= 0 || compressionThreshold > maxWindowSize) {
                throw new IllegalArgumentException("压缩阈值必须在1到最大窗口大小之间");
            }
            if (minRetainedMessages <= 0) {
                throw new IllegalArgumentException("最少保留消息数必须大于0");
            }
            if (compressionRatio <= 0 || compressionRatio >= 1) {
                throw new IllegalArgumentException("压缩比例必须在0到1之间");
            }
            if (maxContextAge <= 0) {
                throw new IllegalArgumentException("最大上下文年龄必须大于0");
            }
        }
    }

    @Override
    public String toString() {
        return String.format("ContextConfig{maxWindowSize=%d, compressionThreshold=%d, " +
                        "minRetainedMessages=%d, compressionRatio=%.2f, enableSmartTruncation=%s, " +
                        "preserveImportantMessages=%s, maxContextAge=%d}",
                maxWindowSize, compressionThreshold, minRetainedMessages,
                compressionRatio, enableSmartTruncation, preserveImportantMessages, maxContextAge);
    }
}