package com.anthropic.claude.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流状态管理器
 * 监控和管理流的状态，支持暂停、恢复和错误恢复
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class StreamStateManager {
    private static final Logger logger = LoggerFactory.getLogger(StreamStateManager.class);

    private final ConcurrentHashMap<String, StreamState> streams = new ConcurrentHashMap<>();
    private final AtomicLong streamIdCounter = new AtomicLong(0);

    /**
     * 创建新的流状态
     *
     * @param streamName 流名称
     * @return 流ID
     */
    public String createStream(String streamName) {
        String streamId = generateStreamId();
        StreamState state = new StreamState(streamId, streamName);
        streams.put(streamId, state);

        logger.info("创建流状态: {} [{}]", streamName, streamId);
        return streamId;
    }

    /**
     * 获取流状态
     *
     * @param streamId 流ID
     * @return 流状态，不存在时返回null
     */
    public StreamState getStreamState(String streamId) {
        return streams.get(streamId);
    }

    /**
     * 更新流状态
     *
     * @param streamId 流ID
     * @param status 新状态
     */
    public void updateStreamStatus(String streamId, StreamStatus status) {
        StreamState state = streams.get(streamId);
        if (state != null) {
            state.setStatus(status);
            state.setLastUpdate(LocalDateTime.now());
            logger.debug("更新流状态: {} -> {}", streamId, status);
        }
    }

    /**
     * 暂停流
     *
     * @param streamId 流ID
     * @return 是否成功暂停
     */
    public boolean pauseStream(String streamId) {
        StreamState state = streams.get(streamId);
        if (state != null && state.getStatus() == StreamStatus.RUNNING) {
            state.setStatus(StreamStatus.PAUSED);
            state.setPauseTime(LocalDateTime.now());
            logger.info("暂停流: {}", streamId);
            return true;
        }
        return false;
    }

    /**
     * 恢复流
     *
     * @param streamId 流ID
     * @return 是否成功恢复
     */
    public boolean resumeStream(String streamId) {
        StreamState state = streams.get(streamId);
        if (state != null && state.getStatus() == StreamStatus.PAUSED) {
            state.setStatus(StreamStatus.RUNNING);
            state.setPauseTime(null);
            logger.info("恢复流: {}", streamId);
            return true;
        }
        return false;
    }

    /**
     * 标记流出错
     *
     * @param streamId 流ID
     * @param error 错误信息
     */
    public void markStreamError(String streamId, Throwable error) {
        StreamState state = streams.get(streamId);
        if (state != null) {
            state.setStatus(StreamStatus.ERROR);
            state.setLastError(error);
            state.setErrorTime(LocalDateTime.now());
            logger.error("流出现错误: {} - {}", streamId, error.getMessage(), error);
        }
    }

    /**
     * 重试出错的流
     *
     * @param streamId 流ID
     * @return 是否可以重试
     */
    public boolean retryStream(String streamId) {
        StreamState state = streams.get(streamId);
        if (state != null && state.getStatus() == StreamStatus.ERROR) {
            state.setStatus(StreamStatus.RUNNING);
            state.incrementRetryCount();
            state.setLastError(null);
            state.setErrorTime(null);
            logger.info("重试流: {} (重试次数: {})", streamId, state.getRetryCount());
            return true;
        }
        return false;
    }

    /**
     * 完成流
     *
     * @param streamId 流ID
     */
    public void completeStream(String streamId) {
        StreamState state = streams.get(streamId);
        if (state != null) {
            state.setStatus(StreamStatus.COMPLETED);
            state.setCompletionTime(LocalDateTime.now());
            logger.info("流完成: {}", streamId);
        }
    }

    /**
     * 关闭流
     *
     * @param streamId 流ID
     */
    public void closeStream(String streamId) {
        StreamState state = streams.remove(streamId);
        if (state != null) {
            state.setStatus(StreamStatus.CLOSED);
            logger.info("关闭流: {}", streamId);
        }
    }

    /**
     * 获取所有活跃流的数量
     *
     * @return 活跃流数量
     */
    public long getActiveStreamCount() {
        return streams.values().stream()
                .filter(state -> state.getStatus() == StreamStatus.RUNNING ||
                                state.getStatus() == StreamStatus.PAUSED)
                .count();
    }

    /**
     * 清理过期流
     *
     * @param expireMinutes 过期时间（分钟）
     * @return 清理的流数量
     */
    public int cleanupExpiredStreams(int expireMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);
        int cleaned = 0;

        for (String streamId : streams.keySet()) {
            StreamState state = streams.get(streamId);
            if (state != null &&
                (state.getStatus() == StreamStatus.COMPLETED || state.getStatus() == StreamStatus.ERROR) &&
                state.getLastUpdate().isBefore(cutoff)) {
                streams.remove(streamId);
                cleaned++;
                logger.debug("清理过期流: {}", streamId);
            }
        }

        if (cleaned > 0) {
            logger.info("清理了 {} 个过期流", cleaned);
        }

        return cleaned;
    }

    /**
     * 关闭所有流
     */
    public void shutdown() {
        logger.info("关闭流状态管理器，流数量: {}", streams.size());
        streams.clear();
    }

    private String generateStreamId() {
        return "stream-" + streamIdCounter.incrementAndGet() + "-" + System.currentTimeMillis();
    }

    /**
     * 流状态枚举
     */
    public enum StreamStatus {
        CREATED,    // 已创建
        RUNNING,    // 运行中
        PAUSED,     // 已暂停
        ERROR,      // 出错
        COMPLETED,  // 已完成
        CLOSED      // 已关闭
    }

    /**
     * 流状态数据类
     */
    public static class StreamState {
        private final String streamId;
        private final String streamName;
        private final LocalDateTime createdTime;
        private volatile StreamStatus status;
        private volatile LocalDateTime lastUpdate;
        private volatile LocalDateTime pauseTime;
        private volatile LocalDateTime errorTime;
        private volatile LocalDateTime completionTime;
        private volatile Throwable lastError;
        private volatile int retryCount = 0;

        public StreamState(String streamId, String streamName) {
            this.streamId = streamId;
            this.streamName = streamName;
            this.createdTime = LocalDateTime.now();
            this.status = StreamStatus.CREATED;
            this.lastUpdate = LocalDateTime.now();
        }

        // Getters and setters
        public String getStreamId() { return streamId; }
        public String getStreamName() { return streamName; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public StreamStatus getStatus() { return status; }
        public void setStatus(StreamStatus status) { this.status = status; }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
        public LocalDateTime getPauseTime() { return pauseTime; }
        public void setPauseTime(LocalDateTime pauseTime) { this.pauseTime = pauseTime; }
        public LocalDateTime getErrorTime() { return errorTime; }
        public void setErrorTime(LocalDateTime errorTime) { this.errorTime = errorTime; }
        public LocalDateTime getCompletionTime() { return completionTime; }
        public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }
        public Throwable getLastError() { return lastError; }
        public void setLastError(Throwable lastError) { this.lastError = lastError; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }

        public boolean isActive() {
            return status == StreamStatus.RUNNING || status == StreamStatus.PAUSED;
        }

        @Override
        public String toString() {
            return String.format("StreamState{id='%s', name='%s', status=%s, retries=%d}",
                    streamId, streamName, status, retryCount);
        }
    }
}