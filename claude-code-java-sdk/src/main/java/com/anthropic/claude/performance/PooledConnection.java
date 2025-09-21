package com.anthropic.claude.performance;

import com.anthropic.claude.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 池化连接
 * 封装Claude CLI进程连接，提供生命周期管理和健康检查
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class PooledConnection {
    private static final Logger logger = LoggerFactory.getLogger(PooledConnection.class);

    private final String id;
    private final ProcessManager processManager;
    private final LocalDateTime createdTime;
    private final AtomicLong usageCount = new AtomicLong(0);
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile LocalDateTime lastUsed;
    private volatile LocalDateTime lastHealthCheck;
    private volatile boolean healthy = true;

    public PooledConnection(String id, ProcessManager processManager) {
        this.id = id;
        this.processManager = processManager;
        this.createdTime = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
        this.lastHealthCheck = LocalDateTime.now();

        logger.debug("创建池化连接: {}", id);
    }

    /**
     * 激活连接
     */
    public void activate() {
        if (closed.get()) {
            throw new IllegalStateException("连接已关闭: " + id);
        }

        active.set(true);
        usageCount.incrementAndGet();
        lastUsed = LocalDateTime.now();

        logger.debug("激活连接: {} (使用次数: {})", id, usageCount.get());
    }

    /**
     * 停用连接
     */
    public void deactivate() {
        active.set(false);
        lastUsed = LocalDateTime.now();

        logger.debug("停用连接: {}", id);
    }

    /**
     * 检查连接健康状态
     *
     * @return 是否健康
     */
    public boolean isHealthy() {
        if (closed.get()) {
            return false;
        }

        // 定期进行健康检查
        LocalDateTime now = LocalDateTime.now();
        if (lastHealthCheck == null || lastHealthCheck.plusMinutes(1).isBefore(now)) {
            performHealthCheck();
            lastHealthCheck = now;
        }

        return healthy;
    }

    /**
     * 获取连接ID
     *
     * @return 连接ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取ProcessManager
     *
     * @return ProcessManager实例
     */
    public ProcessManager getProcessManager() {
        return processManager;
    }

    /**
     * 检查连接是否处于活跃状态
     *
     * @return 是否活跃
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 检查连接是否已关闭
     *
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * 获取使用次数
     *
     * @return 使用次数
     */
    public long getUsageCount() {
        return usageCount.get();
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 获取最后使用时间
     *
     * @return 最后使用时间
     */
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    /**
     * 获取连接年龄（分钟）
     *
     * @return 连接年龄
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(createdTime, LocalDateTime.now()).toMinutes();
    }

    /**
     * 获取空闲时间（分钟）
     *
     * @return 空闲时间
     */
    public long getIdleTimeInMinutes() {
        if (active.get()) {
            return 0;
        }
        return java.time.Duration.between(lastUsed, LocalDateTime.now()).toMinutes();
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        active.set(false);
        healthy = false;

        try {
            // 在实际实现中，这里会关闭Claude CLI进程
            logger.debug("关闭连接相关资源: {}", id);
        } catch (Exception e) {
            logger.error("关闭连接资源失败: {}", id, e);
        }

        logger.debug("连接已关闭: {} (使用次数: {}, 年龄: {}分钟)",
                id, usageCount.get(), getAgeInMinutes());
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            // 在实际实现中，这里会检查Claude CLI进程状态
            // 例如：发送ping命令，检查进程是否响应

            // 检查连接年龄
            if (getAgeInMinutes() > 60) { // 连接超过1小时认为需要更新
                healthy = false;
                logger.debug("连接过期: {} (年龄: {}分钟)", id, getAgeInMinutes());
                return;
            }

            // 检查空闲时间
            if (getIdleTimeInMinutes() > 30) { // 空闲超过30分钟可能需要重新激活
                logger.debug("连接长时间空闲: {} (空闲: {}分钟)", id, getIdleTimeInMinutes());
            }

            // 模拟健康检查
            boolean processHealthy = checkProcessHealth();
            healthy = processHealthy;

            if (!healthy) {
                logger.warn("连接健康检查失败: {}", id);
            }

        } catch (Exception e) {
            healthy = false;
            logger.error("连接健康检查异常: {}", id, e);
        }
    }

    /**
     * 检查进程健康状态
     *
     * @return 进程是否健康
     */
    private boolean checkProcessHealth() {
        try {
            // 在实际实现中，这里会调用processManager检查Claude CLI进程状态
            // 例如：processManager.isProcessAlive()
            return true; // 模拟实现
        } catch (Exception e) {
            logger.error("检查进程健康状态失败: {}", id, e);
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("PooledConnection{id='%s', active=%s, healthy=%s, usage=%d, age=%dm, idle=%dm}",
                id, active.get(), healthy, usageCount.get(), getAgeInMinutes(), getIdleTimeInMinutes());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PooledConnection that = (PooledConnection) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}