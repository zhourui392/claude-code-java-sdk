package com.anthropic.claude.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HookService {
    private static final Logger logger = LoggerFactory.getLogger(HookService.class);

    private final Map<String, List<HookCallback>> hooks;

    public HookService() {
        this.hooks = new ConcurrentHashMap<>();
    }

    public void addHook(String eventType, HookCallback callback) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("事件类型不能为空");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Hook回调不能为null");
        }

        hooks.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(callback);
        logger.debug("添加Hook: {} (总数: {})", eventType, hooks.get(eventType).size());
    }

    public boolean removeHook(String eventType, HookCallback callback) {
        if (eventType == null || callback == null) {
            return false;
        }

        List<HookCallback> callbacks = hooks.get(eventType);
        if (callbacks != null) {
            boolean removed = callbacks.remove(callback);
            if (removed) {
                logger.debug("移除Hook: {} (剩余: {})", eventType, callbacks.size());
                if (callbacks.isEmpty()) {
                    hooks.remove(eventType);
                }
            }
            return removed;
        }
        return false;
    }

    public void removeAllHooks(String eventType) {
        if (eventType != null) {
            List<HookCallback> removed = hooks.remove(eventType);
            if (removed != null) {
                logger.debug("移除所有Hook: {} (移除数量: {})", eventType, removed.size());
            }
        }
    }

    public void clearAllHooks() {
        int totalRemoved = hooks.values().stream().mapToInt(List::size).sum();
        hooks.clear();
        logger.debug("清除所有Hook (总数: {})", totalRemoved);
    }

    public HookResult executeHooks(String eventType, HookContext context) {
        if (eventType == null || context == null) {
            return HookResult.proceed();
        }

        List<HookCallback> callbacks = hooks.get(eventType);
        if (callbacks == null || callbacks.isEmpty()) {
            logger.trace("没有注册Hook: {}", eventType);
            return HookResult.proceed();
        }

        logger.debug("执行Hook: {} (回调数量: {})", eventType, callbacks.size());

        Map<String, Object> combinedData = context.getData();
        StringBuilder messageBuilder = new StringBuilder();

        for (int i = 0; i < callbacks.size(); i++) {
            HookCallback callback = callbacks.get(i);
            try {
                HookResult result = callback.execute(context);

                if (result.getMessage() != null) {
                    if (messageBuilder.length() > 0) {
                        messageBuilder.append("; ");
                    }
                    messageBuilder.append(result.getMessage());
                }

                if (!result.getModifiedData().isEmpty()) {
                    combinedData.putAll(result.getModifiedData());
                    context.getData().putAll(result.getModifiedData());
                }

                if (!result.shouldContinue()) {
                    logger.debug("Hook执行被停止: {} (在第{}个回调处)", eventType, i + 1);
                    return HookResult.stop(combinedData, result.getMessage());
                }

            } catch (Exception e) {
                logger.error("执行Hook回调时出错: {} (第{}个回调)", eventType, i + 1, e);

                String errorMessage = String.format("Hook回调执行失败: %s", e.getMessage());
                return HookResult.stop(combinedData, errorMessage);
            }
        }

        String finalMessage = messageBuilder.length() > 0 ? messageBuilder.toString() : null;
        logger.debug("Hook执行完成: {} (成功执行{}个回调)", eventType, callbacks.size());

        return HookResult.proceed(combinedData, finalMessage);
    }

    public boolean hasHooks(String eventType) {
        List<HookCallback> callbacks = hooks.get(eventType);
        return callbacks != null && !callbacks.isEmpty();
    }

    public int getHookCount(String eventType) {
        List<HookCallback> callbacks = hooks.get(eventType);
        return callbacks != null ? callbacks.size() : 0;
    }

    public List<String> getRegisteredEventTypes() {
        return new ArrayList<>(hooks.keySet());
    }

    public int getTotalHookCount() {
        return hooks.values().stream().mapToInt(List::size).sum();
    }

    public Map<String, Integer> getHookStatistics() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        hooks.forEach((eventType, callbacks) -> stats.put(eventType, callbacks.size()));
        return stats;
    }
}