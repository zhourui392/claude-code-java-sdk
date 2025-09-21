package com.anthropic.claude.client;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话管理器
 * 负责会话的创建、管理和清理
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private final ClaudeCodeOptions options;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong sessionCounter = new AtomicLong(0);

    public SessionManager(ClaudeCodeOptions options) {
        this.options = options;
        startSessionCleanupTask();
        logger.info("会话管理器已启动");
    }

    /**
     * 创建新会话
     *
     * @return 会话ID
     */
    public String createSession() {
        String sessionId = generateSessionId();
        Session session = new Session(sessionId);
        sessions.put(sessionId, session);

        logger.info("创建新会话: {}", sessionId);
        return sessionId;
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话ID
     * @return 会话对象，不存在时返回null
     */
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 检查会话是否存在且活跃
     *
     * @param sessionId 会话ID
     * @return 是否活跃
     */
    public boolean isSessionActive(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null && session.isActive();
    }

    /**
     * 获取会话状态
     *
     * @param sessionId 会话ID
     * @return 会话状态
     */
    public ClaudeSDKClient.SessionState getSessionState(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return new ClaudeSDKClient.SessionState(false, null, 0);
        }

        return new ClaudeSDKClient.SessionState(
                session.isActive(),
                sessionId,
                session.getMessageCount()
        );
    }

    /**
     * 更新会话活动时间
     *
     * @param sessionId 会话ID
     */
    public void updateSessionActivity(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.updateLastActivity();
            logger.debug("更新会话活动时间: {}", sessionId);
        }
    }

    /**
     * 增加会话消息计数
     *
     * @param sessionId 会话ID
     */
    public void incrementMessageCount(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.incrementMessageCount();
            updateSessionActivity(sessionId);
        }
    }

    /**
     * 清理指定会话
     *
     * @param sessionId 会话ID
     * @return 是否成功清理
     */
    public boolean cleanupSession(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session != null) {
            session.setActive(false);
            logger.info("清理会话: {}", sessionId);
            return true;
        }
        return false;
    }

    /**
     * 清理所有过期会话
     *
     * @return 清理的会话数量
     */
    public int cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = options.getTimeout().toMillis() * 2; // 会话超时时间为查询超时的2倍

        int cleanedCount = 0;
        for (String sessionId : sessions.keySet()) {
            Session session = sessions.get(sessionId);
            if (session != null && currentTime - session.getLastActivity() > sessionTimeout) {
                if (cleanupSession(sessionId)) {
                    cleanedCount++;
                }
            }
        }

        if (cleanedCount > 0) {
            logger.info("清理了 {} 个过期会话", cleanedCount);
        }

        return cleanedCount;
    }

    /**
     * 获取活跃会话数量
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(Session::isActive)
                .count();
    }

    /**
     * 获取总会话数量
     *
     * @return 总会话数量
     */
    public int getTotalSessionCount() {
        return sessions.size();
    }

    /**
     * 检查会话管理器健康状态
     *
     * @return 是否健康
     */
    public boolean isHealthy() {
        return !scheduler.isShutdown();
    }

    /**
     * 关闭会话管理器
     */
    public void shutdown() {
        logger.info("正在关闭会话管理器...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 清理所有会话
        sessions.clear();
        logger.info("会话管理器已关闭");
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        long counter = sessionCounter.incrementAndGet();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("session-%d-%s", counter, uuid);
    }

    /**
     * 启动会话清理任务
     */
    private void startSessionCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredSessions();
            } catch (Exception e) {
                logger.error("会话清理任务出错", e);
            }
        }, 60, 60, TimeUnit.SECONDS); // 每分钟执行一次清理

        logger.debug("会话清理任务已启动");
    }

    /**
     * 会话数据类
     */
    public static class Session {
        private final String sessionId;
        private final LocalDateTime createdAt;
        private volatile long lastActivity;
        private volatile boolean active;
        private final AtomicLong messageCount = new AtomicLong(0);

        public Session(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = LocalDateTime.now();
            this.lastActivity = System.currentTimeMillis();
            this.active = true;
        }

        public String getSessionId() { return sessionId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public long getLastActivity() { return lastActivity; }
        public boolean isActive() { return active; }
        public long getMessageCount() { return messageCount.get(); }

        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void incrementMessageCount() {
            messageCount.incrementAndGet();
        }

        @Override
        public String toString() {
            return String.format("Session{id='%s', created=%s, active=%s, messages=%d}",
                    sessionId, createdAt, active, messageCount.get());
        }
    }
}