package com.anthropic.claude.client;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中断处理器
 * 负责处理查询中断操作
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class InterruptHandler {
    private static final Logger logger = LoggerFactory.getLogger(InterruptHandler.class);

    private final ConcurrentHashMap<String, InterruptContext> activeInterrupts = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public InterruptHandler() {
        logger.debug("中断处理器已创建");
    }

    /**
     * 执行中断操作
     *
     * @param sessionId 会话ID
     */
    public void performInterrupt(String sessionId) {
        if (shutdown.get()) {
            throw new ClaudeCodeException("中断处理器已关闭");
        }

        logger.info("执行会话中断: {}", sessionId);

        try {
            InterruptContext context = new InterruptContext(sessionId);
            activeInterrupts.put(sessionId, context);

            // 执行中断逻辑
            executeInterrupt(context);

            logger.info("会话中断完成: {}", sessionId);

        } catch (Exception e) {
            logger.error("执行中断失败: {}", sessionId, e);
            throw new ClaudeCodeException("中断操作失败: " + e.getMessage(), e);
        } finally {
            activeInterrupts.remove(sessionId);
        }
    }

    /**
     * 检查会话是否正在被中断
     *
     * @param sessionId 会话ID
     * @return 是否正在中断
     */
    public boolean isInterrupting(String sessionId) {
        return activeInterrupts.containsKey(sessionId);
    }

    /**
     * 获取活跃中断数量
     *
     * @return 中断数量
     */
    public int getActiveInterruptCount() {
        return activeInterrupts.size();
    }

    /**
     * 等待指定会话的中断完成
     *
     * @param sessionId 会话ID
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否在超时前完成
     */
    public boolean waitForInterruptCompletion(String sessionId, long timeout, TimeUnit unit) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = unit.toMillis(timeout);

        while (isInterrupting(sessionId)) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                logger.warn("等待中断完成超时: {}", sessionId);
                return false;
            }

            try {
                Thread.sleep(100); // 100ms 检查间隔
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return true;
    }

    /**
     * 优雅中断所有活跃会话
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否所有中断都已完成
     */
    public boolean interruptAllSessions(long timeout, TimeUnit unit) {
        if (activeInterrupts.isEmpty()) {
            return true;
        }

        logger.info("开始中断所有活跃会话，数量: {}", activeInterrupts.size());

        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = activeInterrupts.keySet().stream()
                .map(sessionId -> CompletableFuture.runAsync(() -> performInterrupt(sessionId)))
                .toArray(size -> new CompletableFuture[size]);

        try {
            CompletableFuture.allOf(futures)
                    .get(timeout, unit);
            return true;
        } catch (Exception e) {
            logger.error("批量中断操作失败", e);
            return false;
        }
    }

    /**
     * 关闭中断处理器
     */
    public void shutdown() {
        if (shutdown.getAndSet(true)) {
            return;
        }

        logger.info("正在关闭中断处理器...");

        // 中断所有活跃会话
        if (!activeInterrupts.isEmpty()) {
            interruptAllSessions(5, TimeUnit.SECONDS);
        }

        activeInterrupts.clear();
        logger.info("中断处理器已关闭");
    }

    /**
     * 执行具体的中断逻辑
     */
    private void executeInterrupt(InterruptContext context) {
        String sessionId = context.getSessionId();
        logger.debug("开始执行中断逻辑: {}", sessionId);

        // 1. 设置中断标志
        context.setInterrupted(true);

        // 2. 停止正在进行的操作
        stopOngoingOperations(context);

        // 3. 清理相关资源
        cleanupResources(context);

        // 4. 发送中断信号
        sendInterruptSignal(context);

        logger.debug("中断逻辑执行完成: {}", sessionId);
    }

    /**
     * 停止正在进行的操作
     */
    private void stopOngoingOperations(InterruptContext context) {
        // 在实际实现中，这里会：
        // 1. 终止Claude CLI进程
        // 2. 关闭网络连接
        // 3. 停止后台任务
        logger.debug("停止正在进行的操作: {}", context.getSessionId());
    }

    /**
     * 清理相关资源
     */
    private void cleanupResources(InterruptContext context) {
        // 在实际实现中，这里会：
        // 1. 清理临时文件
        // 2. 释放内存
        // 3. 关闭文件句柄
        logger.debug("清理相关资源: {}", context.getSessionId());
    }

    /**
     * 发送中断信号
     */
    private void sendInterruptSignal(InterruptContext context) {
        // 在实际实现中，这里会向Claude CLI发送SIGINT或类似信号
        logger.debug("发送中断信号: {}", context.getSessionId());
    }

    /**
     * 中断上下文
     */
    private static class InterruptContext {
        private final String sessionId;
        private final long startTime;
        private volatile boolean interrupted;

        public InterruptContext(String sessionId) {
            this.sessionId = sessionId;
            this.startTime = System.currentTimeMillis();
            this.interrupted = false;
        }

        public String getSessionId() { return sessionId; }
        public long getStartTime() { return startTime; }
        public boolean isInterrupted() { return interrupted; }
        public void setInterrupted(boolean interrupted) { this.interrupted = interrupted; }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        @Override
        public String toString() {
            return String.format("InterruptContext{sessionId='%s', elapsed=%dms, interrupted=%s}",
                    sessionId, getElapsedTime(), interrupted);
        }
    }
}