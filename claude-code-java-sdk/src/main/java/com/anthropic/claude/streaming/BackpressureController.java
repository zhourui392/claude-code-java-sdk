package com.anthropic.claude.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 背压控制器
 * 管理流处理的背压机制，防止生产者过快导致内存溢出
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class BackpressureController {
    private static final Logger logger = LoggerFactory.getLogger(BackpressureController.class);

    private final int maxBufferSize;
    private final int highWaterMark;
    private final int lowWaterMark;
    private final Semaphore permits;
    private final AtomicInteger currentBufferSize = new AtomicInteger(0);
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalDropped = new AtomicLong(0);

    private volatile BackpressureStrategy strategy = BackpressureStrategy.BLOCK;
    private volatile boolean backpressureActive = false;

    public BackpressureController(int maxBufferSize) {
        this(maxBufferSize, (int) (maxBufferSize * 0.8), (int) (maxBufferSize * 0.3));
    }

    public BackpressureController(int maxBufferSize, int highWaterMark, int lowWaterMark) {
        this.maxBufferSize = maxBufferSize;
        this.highWaterMark = highWaterMark;
        this.lowWaterMark = lowWaterMark;
        this.permits = new Semaphore(maxBufferSize);

        logger.info("背压控制器已初始化 - 最大缓冲: {}, 高水位: {}, 低水位: {}",
                maxBufferSize, highWaterMark, lowWaterMark);
    }

    /**
     * 请求处理许可
     *
     * @return CompletableFuture表示许可获取结果
     */
    public CompletableFuture<Boolean> requestPermit() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (strategy) {
                    case BLOCK:
                        permits.acquire();
                        incrementBuffer();
                        return true;

                    case DROP_OLDEST:
                        if (permits.tryAcquire()) {
                            incrementBuffer();
                            return true;
                        } else {
                            totalDropped.incrementAndGet();
                            logger.debug("背压激活 - 丢弃元素 (DROP_OLDEST)");
                            return false;
                        }

                    case DROP_LATEST:
                        if (currentBufferSize.get() >= maxBufferSize) {
                            totalDropped.incrementAndGet();
                            logger.debug("背压激活 - 丢弃元素 (DROP_LATEST)");
                            return false;
                        } else {
                            permits.acquire();
                            incrementBuffer();
                            return true;
                        }

                    case BUFFER:
                        // 允许超过缓冲区大小，但记录警告
                        permits.acquire();
                        incrementBuffer();
                        if (currentBufferSize.get() > maxBufferSize) {
                            logger.warn("缓冲区超出限制: {}/{}", currentBufferSize.get(), maxBufferSize);
                        }
                        return true;

                    default:
                        permits.acquire();
                        incrementBuffer();
                        return true;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取许可被中断", e);
                return false;
            }
        });
    }

    /**
     * 释放许可
     */
    public void releasePermit() {
        permits.release();
        decrementBuffer();
        totalProcessed.incrementAndGet();

        // 检查是否需要停用背压
        if (backpressureActive && currentBufferSize.get() <= lowWaterMark) {
            backpressureActive = false;
            logger.info("背压已停用 - 当前缓冲: {}/{}", currentBufferSize.get(), maxBufferSize);
        }
    }

    /**
     * 获取当前缓冲区大小
     *
     * @return 缓冲区大小
     */
    public int getCurrentBufferSize() {
        return currentBufferSize.get();
    }

    /**
     * 获取可用许可数
     *
     * @return 可用许可数
     */
    public int getAvailablePermits() {
        return permits.availablePermits();
    }

    /**
     * 检查是否处于背压状态
     *
     * @return 是否处于背压状态
     */
    public boolean isBackpressureActive() {
        return backpressureActive;
    }

    /**
     * 设置背压策略
     *
     * @param strategy 背压策略
     */
    public void setBackpressureStrategy(BackpressureStrategy strategy) {
        this.strategy = strategy;
        logger.info("设置背压策略: {}", strategy);
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    public BackpressureStats getStats() {
        return new BackpressureStats(
                maxBufferSize,
                currentBufferSize.get(),
                permits.availablePermits(),
                totalProcessed.get(),
                totalDropped.get(),
                backpressureActive,
                strategy
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalProcessed.set(0);
        totalDropped.set(0);
        logger.info("重置背压统计信息");
    }

    private void incrementBuffer() {
        int current = currentBufferSize.incrementAndGet();

        // 检查是否需要激活背压
        if (!backpressureActive && current >= highWaterMark) {
            backpressureActive = true;
            logger.warn("背压已激活 - 当前缓冲: {}/{}", current, maxBufferSize);
        }
    }

    private void decrementBuffer() {
        currentBufferSize.decrementAndGet();
    }

    /**
     * 背压策略枚举
     */
    public enum BackpressureStrategy {
        BLOCK,       // 阻塞生产者
        DROP_OLDEST, // 丢弃最旧元素
        DROP_LATEST, // 丢弃最新元素
        BUFFER       // 允许缓冲区增长（可能导致内存问题）
    }

    /**
     * 背压统计信息
     */
    public static class BackpressureStats {
        private final int maxBufferSize;
        private final int currentBufferSize;
        private final int availablePermits;
        private final long totalProcessed;
        private final long totalDropped;
        private final boolean backpressureActive;
        private final BackpressureStrategy strategy;

        public BackpressureStats(int maxBufferSize, int currentBufferSize, int availablePermits,
                                long totalProcessed, long totalDropped, boolean backpressureActive,
                                BackpressureStrategy strategy) {
            this.maxBufferSize = maxBufferSize;
            this.currentBufferSize = currentBufferSize;
            this.availablePermits = availablePermits;
            this.totalProcessed = totalProcessed;
            this.totalDropped = totalDropped;
            this.backpressureActive = backpressureActive;
            this.strategy = strategy;
        }

        // Getters
        public int getMaxBufferSize() { return maxBufferSize; }
        public int getCurrentBufferSize() { return currentBufferSize; }
        public int getAvailablePermits() { return availablePermits; }
        public long getTotalProcessed() { return totalProcessed; }
        public long getTotalDropped() { return totalDropped; }
        public boolean isBackpressureActive() { return backpressureActive; }
        public BackpressureStrategy getStrategy() { return strategy; }

        public double getBufferUtilization() {
            return (double) currentBufferSize / maxBufferSize;
        }

        public double getDropRate() {
            long total = totalProcessed + totalDropped;
            return total > 0 ? (double) totalDropped / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("BackpressureStats{buffer=%d/%d (%.1f%%), processed=%d, dropped=%d (%.2f%%), active=%s, strategy=%s}",
                    currentBufferSize, maxBufferSize, getBufferUtilization() * 100,
                    totalProcessed, totalDropped, getDropRate() * 100,
                    backpressureActive, strategy);
        }
    }
}