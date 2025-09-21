package com.anthropic.claude.performance;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器
 * 统一管理各种类型的缓存，提供查询结果缓存、配置缓存等
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    private final Cache<String, Object> queryResultCache;
    private final Cache<String, Object> configCache;
    private final Cache<String, Object> sessionCache;
    private final ConcurrentHashMap<String, Cache<String, Object>> customCaches = new ConcurrentHashMap<>();

    public CacheManager() {
        this(CacheConfig.defaultConfig());
    }

    public CacheManager(CacheConfig config) {
        // 查询结果缓存 - 较大容量，较长过期时间
        this.queryResultCache = Caffeine.newBuilder()
                .maximumSize(config.getQueryCacheSize())
                .expireAfterWrite(Duration.ofMinutes(config.getQueryCacheExpireMinutes()))
                .recordStats()
                .build();

        // 配置缓存 - 中等容量，很长过期时间
        this.configCache = Caffeine.newBuilder()
                .maximumSize(config.getConfigCacheSize())
                .expireAfterWrite(Duration.ofHours(config.getConfigCacheExpireHours()))
                .recordStats()
                .build();

        // 会话缓存 - 中等容量，中等过期时间
        this.sessionCache = Caffeine.newBuilder()
                .maximumSize(config.getSessionCacheSize())
                .expireAfterAccess(Duration.ofMinutes(config.getSessionCacheExpireMinutes()))
                .recordStats()
                .build();

        logger.info("缓存管理器已初始化 - 查询缓存: {}, 配置缓存: {}, 会话缓存: {}",
                config.getQueryCacheSize(), config.getConfigCacheSize(), config.getSessionCacheSize());
    }

    /**
     * 获取查询结果缓存
     *
     * @param key 缓存键
     * @return 缓存值，不存在时返回null
     */
    public Object getQueryResult(String key) {
        Object result = queryResultCache.getIfPresent(key);
        if (result != null) {
            logger.debug("查询缓存命中: {}", key);
        }
        return result;
    }

    /**
     * 设置查询结果缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public void putQueryResult(String key, Object value) {
        queryResultCache.put(key, value);
        logger.debug("查询结果已缓存: {}", key);
    }

    /**
     * 获取配置缓存
     *
     * @param key 缓存键
     * @return 配置值，不存在时返回null
     */
    public Object getConfig(String key) {
        Object config = configCache.getIfPresent(key);
        if (config != null) {
            logger.debug("配置缓存命中: {}", key);
        }
        return config;
    }

    /**
     * 设置配置缓存
     *
     * @param key 配置键
     * @param value 配置值
     */
    public void putConfig(String key, Object value) {
        configCache.put(key, value);
        logger.debug("配置已缓存: {}", key);
    }

    /**
     * 获取会话缓存
     *
     * @param key 会话键
     * @return 会话值，不存在时返回null
     */
    public Object getSession(String key) {
        Object session = sessionCache.getIfPresent(key);
        if (session != null) {
            logger.debug("会话缓存命中: {}", key);
        }
        return session;
    }

    /**
     * 设置会话缓存
     *
     * @param key 会话键
     * @param value 会话值
     */
    public void putSession(String key, Object value) {
        sessionCache.put(key, value);
        logger.debug("会话已缓存: {}", key);
    }

    /**
     * 创建自定义缓存
     *
     * @param cacheName 缓存名称
     * @param maxSize 最大大小
     * @param expireDuration 过期时间
     * @return 自定义缓存
     */
    public Cache<String, Object> createCustomCache(String cacheName, long maxSize, Duration expireDuration) {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireDuration)
                .recordStats()
                .build();

        customCaches.put(cacheName, cache);
        logger.info("创建自定义缓存: {} (大小: {}, 过期: {})", cacheName, maxSize, expireDuration);
        return cache;
    }

    /**
     * 获取自定义缓存
     *
     * @param cacheName 缓存名称
     * @return 自定义缓存，不存在时返回null
     */
    public Cache<String, Object> getCustomCache(String cacheName) {
        return customCaches.get(cacheName);
    }

    /**
     * 清空指定缓存
     *
     * @param cacheType 缓存类型
     */
    public void clearCache(CacheType cacheType) {
        switch (cacheType) {
            case QUERY_RESULT:
                queryResultCache.invalidateAll();
                logger.info("查询结果缓存已清空");
                break;
            case CONFIG:
                configCache.invalidateAll();
                logger.info("配置缓存已清空");
                break;
            case SESSION:
                sessionCache.invalidateAll();
                logger.info("会话缓存已清空");
                break;
            case ALL:
                queryResultCache.invalidateAll();
                configCache.invalidateAll();
                sessionCache.invalidateAll();
                customCaches.values().forEach(Cache::invalidateAll);
                logger.info("所有缓存已清空");
                break;
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    public CacheManagerStats getStats() {
        CacheStats queryStats = queryResultCache.stats();
        CacheStats configStats = configCache.stats();
        CacheStats sessionStats = sessionCache.stats();

        return new CacheManagerStats(
                queryResultCache.estimatedSize(),
                configCache.estimatedSize(),
                sessionCache.estimatedSize(),
                customCaches.size(),
                queryStats.hitRate(),
                configStats.hitRate(),
                sessionStats.hitRate(),
                queryStats.evictionCount(),
                configStats.evictionCount(),
                sessionStats.evictionCount()
        );
    }

    /**
     * 执行缓存清理
     */
    public void cleanup() {
        queryResultCache.cleanUp();
        configCache.cleanUp();
        sessionCache.cleanUp();
        customCaches.values().forEach(Cache::cleanUp);
        logger.debug("缓存清理完成");
    }

    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        logger.info("正在关闭缓存管理器...");
        clearCache(CacheType.ALL);
        customCaches.clear();
        logger.info("缓存管理器已关闭");
    }

    /**
     * 缓存类型枚举
     */
    public enum CacheType {
        QUERY_RESULT,
        CONFIG,
        SESSION,
        ALL
    }

    /**
     * 缓存配置类
     */
    public static class CacheConfig {
        private final long queryCacheSize;
        private final long configCacheSize;
        private final long sessionCacheSize;
        private final int queryCacheExpireMinutes;
        private final int configCacheExpireHours;
        private final int sessionCacheExpireMinutes;

        private CacheConfig(Builder builder) {
            this.queryCacheSize = builder.queryCacheSize;
            this.configCacheSize = builder.configCacheSize;
            this.sessionCacheSize = builder.sessionCacheSize;
            this.queryCacheExpireMinutes = builder.queryCacheExpireMinutes;
            this.configCacheExpireHours = builder.configCacheExpireHours;
            this.sessionCacheExpireMinutes = builder.sessionCacheExpireMinutes;
        }

        public static CacheConfig defaultConfig() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public long getQueryCacheSize() { return queryCacheSize; }
        public long getConfigCacheSize() { return configCacheSize; }
        public long getSessionCacheSize() { return sessionCacheSize; }
        public int getQueryCacheExpireMinutes() { return queryCacheExpireMinutes; }
        public int getConfigCacheExpireHours() { return configCacheExpireHours; }
        public int getSessionCacheExpireMinutes() { return sessionCacheExpireMinutes; }

        public static class Builder {
            private long queryCacheSize = 1000;
            private long configCacheSize = 100;
            private long sessionCacheSize = 500;
            private int queryCacheExpireMinutes = 30;
            private int configCacheExpireHours = 24;
            private int sessionCacheExpireMinutes = 60;

            public Builder queryCacheSize(long queryCacheSize) {
                this.queryCacheSize = queryCacheSize;
                return this;
            }

            public Builder configCacheSize(long configCacheSize) {
                this.configCacheSize = configCacheSize;
                return this;
            }

            public Builder sessionCacheSize(long sessionCacheSize) {
                this.sessionCacheSize = sessionCacheSize;
                return this;
            }

            public Builder queryCacheExpireMinutes(int queryCacheExpireMinutes) {
                this.queryCacheExpireMinutes = queryCacheExpireMinutes;
                return this;
            }

            public Builder configCacheExpireHours(int configCacheExpireHours) {
                this.configCacheExpireHours = configCacheExpireHours;
                return this;
            }

            public Builder sessionCacheExpireMinutes(int sessionCacheExpireMinutes) {
                this.sessionCacheExpireMinutes = sessionCacheExpireMinutes;
                return this;
            }

            public CacheConfig build() {
                return new CacheConfig(this);
            }
        }
    }

    /**
     * 缓存管理器统计信息
     */
    public static class CacheManagerStats {
        private final long queryResultCacheSize;
        private final long configCacheSize;
        private final long sessionCacheSize;
        private final int customCacheCount;
        private final double queryResultHitRate;
        private final double configHitRate;
        private final double sessionHitRate;
        private final long queryResultEvictions;
        private final long configEvictions;
        private final long sessionEvictions;

        public CacheManagerStats(long queryResultCacheSize, long configCacheSize, long sessionCacheSize,
                                int customCacheCount, double queryResultHitRate, double configHitRate,
                                double sessionHitRate, long queryResultEvictions, long configEvictions,
                                long sessionEvictions) {
            this.queryResultCacheSize = queryResultCacheSize;
            this.configCacheSize = configCacheSize;
            this.sessionCacheSize = sessionCacheSize;
            this.customCacheCount = customCacheCount;
            this.queryResultHitRate = queryResultHitRate;
            this.configHitRate = configHitRate;
            this.sessionHitRate = sessionHitRate;
            this.queryResultEvictions = queryResultEvictions;
            this.configEvictions = configEvictions;
            this.sessionEvictions = sessionEvictions;
        }

        // Getters
        public long getQueryResultCacheSize() { return queryResultCacheSize; }
        public long getConfigCacheSize() { return configCacheSize; }
        public long getSessionCacheSize() { return sessionCacheSize; }
        public int getCustomCacheCount() { return customCacheCount; }
        public double getQueryResultHitRate() { return queryResultHitRate; }
        public double getConfigHitRate() { return configHitRate; }
        public double getSessionHitRate() { return sessionHitRate; }
        public long getQueryResultEvictions() { return queryResultEvictions; }
        public long getConfigEvictions() { return configEvictions; }
        public long getSessionEvictions() { return sessionEvictions; }

        public long getTotalCacheSize() {
            return queryResultCacheSize + configCacheSize + sessionCacheSize;
        }

        public double getOverallHitRate() {
            return (queryResultHitRate + configHitRate + sessionHitRate) / 3.0;
        }

        @Override
        public String toString() {
            return String.format("CacheManagerStats{totalSize=%d, customCaches=%d, overallHitRate=%.2f%%, totalEvictions=%d}",
                    getTotalCacheSize(), customCacheCount, getOverallHitRate() * 100,
                    queryResultEvictions + configEvictions + sessionEvictions);
        }
    }
}