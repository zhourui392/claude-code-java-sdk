package com.anthropic.claude.performance;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 连接池管理器
 * 管理Claude CLI进程池，提供连接复用和健康检查
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ConnectionPoolManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);

    private final int minPoolSize;
    private final int maxPoolSize;
    private final long connectionTimeout;
    private final long healthCheckInterval;
    private final BlockingQueue<PooledConnection> availableConnections;
    private final ConcurrentHashMap<String, PooledConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicLong connectionCounter = new AtomicLong(0);
    private final ScheduledExecutorService healthCheckExecutor;

    private final ProcessManager processManager;
    private volatile boolean shutdown = false;

    public ConnectionPoolManager(ClaudeCodeOptions options) {
        this.minPoolSize = options.getMinPoolSize();
        this.maxPoolSize = options.getMaxPoolSize();
        this.connectionTimeout = options.getConnectionTimeout();
        this.healthCheckInterval = options.getHealthCheckInterval();
        this.availableConnections = new LinkedBlockingQueue<>(maxPoolSize);
        this.processManager = new ProcessManager(options.getTimeout(), options.getEnvironment());
        this.healthCheckExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "connection-pool-health-checker");
            thread.setDaemon(true);
            return thread;
        });

        initializePool();
        startHealthChecker();

        logger.info("连接池管理器已初始化 - 最小连接: {}, 最大连接: {}", minPoolSize, maxPoolSize);
    }

    /**
     * 获取连接
     *
     * @return 连接的CompletableFuture
     */
    public CompletableFuture<PooledConnection> getConnection() {
        if (shutdown) {
            CompletableFuture<PooledConnection> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("连接池已关闭"));
            return failedFuture;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 首先尝试从可用连接中获取
                PooledConnection connection = availableConnections.poll();

                if (connection != null && connection.isHealthy()) {
                    connection.activate();
                    activeConnections.put(connection.getId(), connection);
                    logger.debug("复用连接: {}", connection.getId());
                    return connection;
                }

                // 如果没有可用连接，创建新连接
                if (totalConnections.get() < maxPoolSize) {
                    connection = createNewConnection();
                    if (connection != null) {
                        connection.activate();
                        activeConnections.put(connection.getId(), connection);
                        totalConnections.incrementAndGet();
                        logger.debug("创建新连接: {}", connection.getId());
                        return connection;
                    }
                }

                // 等待连接可用
                connection = availableConnections.poll(connectionTimeout, TimeUnit.MILLISECONDS);
                if (connection != null && connection.isHealthy()) {
                    connection.activate();
                    activeConnections.put(connection.getId(), connection);
                    return connection;
                }

                throw new TimeoutException("获取连接超时");

            } catch (Exception e) {
                logger.error("获取连接失败", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 释放连接
     *
     * @param connection 要释放的连接
     */
    public void releaseConnection(PooledConnection connection) {
        if (connection == null) {
            return;
        }

        activeConnections.remove(connection.getId());
        connection.deactivate();

        if (connection.isHealthy() && !shutdown) {
            availableConnections.offer(connection);
            logger.debug("释放连接: {}", connection.getId());
        } else {
            closeConnection(connection);
        }
    }

    /**
     * 获取连接池统计信息
     *
     * @return 统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
                totalConnections.get(),
                activeConnections.size(),
                availableConnections.size(),
                maxPoolSize,
                minPoolSize
        );
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }

        shutdown = true;
        logger.info("正在关闭连接池...");

        // 停止健康检查
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭所有连接
        for (PooledConnection connection : activeConnections.values()) {
            closeConnection(connection);
        }
        activeConnections.clear();

        while (!availableConnections.isEmpty()) {
            PooledConnection connection = availableConnections.poll();
            if (connection != null) {
                closeConnection(connection);
            }
        }

        logger.info("连接池已关闭");
    }

    /**
     * 初始化连接池
     */
    private void initializePool() {
        for (int i = 0; i < minPoolSize; i++) {
            PooledConnection connection = createNewConnection();
            if (connection != null) {
                availableConnections.offer(connection);
                totalConnections.incrementAndGet();
            }
        }
        logger.info("连接池初始化完成 - 初始连接数: {}", totalConnections.get());
    }

    /**
     * 创建新连接
     */
    private PooledConnection createNewConnection() {
        try {
            String connectionId = "conn-" + connectionCounter.incrementAndGet();
            return new PooledConnection(connectionId, processManager);
        } catch (Exception e) {
            logger.error("创建连接失败", e);
            return null;
        }
    }

    /**
     * 关闭连接
     */
    private void closeConnection(PooledConnection connection) {
        try {
            connection.close();
            totalConnections.decrementAndGet();
            logger.debug("关闭连接: {}", connection.getId());
        } catch (Exception e) {
            logger.error("关闭连接失败: {}", connection.getId(), e);
        }
    }

    /**
     * 启动健康检查器
     */
    private void startHealthChecker() {
        healthCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                logger.error("健康检查失败", e);
            }
        }, healthCheckInterval, healthCheckInterval, TimeUnit.MILLISECONDS);

        logger.debug("健康检查器已启动 - 检查间隔: {}ms", healthCheckInterval);
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        int checkedConnections = 0;
        int unhealthyConnections = 0;

        // 检查可用连接
        PooledConnection[] connections = availableConnections.toArray(new PooledConnection[0]);
        for (PooledConnection connection : connections) {
            checkedConnections++;
            if (!connection.isHealthy()) {
                availableConnections.remove(connection);
                closeConnection(connection);
                unhealthyConnections++;
            }
        }

        // 检查活跃连接
        for (PooledConnection connection : activeConnections.values()) {
            checkedConnections++;
            if (!connection.isHealthy()) {
                // 活跃连接不健康时只记录，等待释放时处理
                unhealthyConnections++;
                logger.warn("活跃连接不健康: {}", connection.getId());
            }
        }

        if (unhealthyConnections > 0) {
            logger.info("健康检查完成 - 检查: {}, 不健康: {}", checkedConnections, unhealthyConnections);
        } else {
            logger.debug("健康检查完成 - 检查: {}, 全部健康", checkedConnections);
        }

        // 补充连接到最小数量
        ensureMinimumConnections();
    }

    /**
     * 确保最小连接数量
     */
    private void ensureMinimumConnections() {
        int currentTotal = totalConnections.get();
        if (currentTotal < minPoolSize) {
            int needed = minPoolSize - currentTotal;
            for (int i = 0; i < needed; i++) {
                PooledConnection connection = createNewConnection();
                if (connection != null) {
                    availableConnections.offer(connection);
                    totalConnections.incrementAndGet();
                }
            }
            logger.debug("补充连接到最小数量 - 新增: {}", needed);
        }
    }

    /**
     * 连接池统计信息
     */
    public static class PoolStats {
        private final int totalConnections;
        private final int activeConnections;
        private final int availableConnections;
        private final int maxPoolSize;
        private final int minPoolSize;

        public PoolStats(int totalConnections, int activeConnections, int availableConnections,
                        int maxPoolSize, int minPoolSize) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.availableConnections = availableConnections;
            this.maxPoolSize = maxPoolSize;
            this.minPoolSize = minPoolSize;
        }

        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getAvailableConnections() { return availableConnections; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getMinPoolSize() { return minPoolSize; }

        public double getUtilization() {
            return maxPoolSize > 0 ? (double) totalConnections / maxPoolSize : 0.0;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{total=%d, active=%d, available=%d, utilization=%.1f%%, max=%d, min=%d}",
                    totalConnections, activeConnections, availableConnections,
                    getUtilization() * 100, maxPoolSize, minPoolSize);
        }
    }
}