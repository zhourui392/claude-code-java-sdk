package com.anthropic.claude.tools;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内MCP服务器
 * 管理自定义工具的注册、发现和执行
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class MCPServer {
    private static final Logger logger = LoggerFactory.getLogger(MCPServer.class);

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final ToolExecutor toolExecutor;
    private final Set<Object> registeredInstances = new HashSet<>();

    public MCPServer() {
        this.toolExecutor = new ToolExecutor();
        logger.info("MCP服务器已启动");
    }

    /**
     * 注册工具类实例
     * 自动扫描带有@Tool注解的方法
     *
     * @param instance 包含工具方法的实例
     */
    public synchronized void registerTools(Object instance) {
        if (registeredInstances.contains(instance)) {
            logger.warn("实例已注册，跳过: {}", instance.getClass().getName());
            return;
        }

        Class<?> clazz = instance.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        int registeredCount = 0;
        for (Method method : methods) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(instance, method, toolAnnotation);
                registeredCount++;
            }
        }

        if (registeredCount > 0) {
            registeredInstances.add(instance);
            logger.info("从类 {} 注册了 {} 个工具", clazz.getSimpleName(), registeredCount);
        } else {
            logger.warn("类 {} 中未找到@Tool注解的方法", clazz.getSimpleName());
        }
    }

    /**
     * 注册单个工具方法
     */
    private void registerTool(Object instance, Method method, Tool toolAnnotation) {
        String toolName = toolAnnotation.name().isEmpty() ?
                method.getName() : toolAnnotation.name();

        if (tools.containsKey(toolName)) {
            throw new ClaudeCodeException("工具名称冲突: " + toolName);
        }

        ToolDefinition toolDefinition = new ToolDefinition(
                toolName,
                toolAnnotation.description(),
                method,
                instance,
                toolAnnotation.async(),
                toolAnnotation.timeout(),
                toolAnnotation.priority()
        );

        tools.put(toolName, toolDefinition);
        logger.debug("注册工具: {} -> {}", toolName, method.getName());
    }

    /**
     * 手动注册工具
     */
    public synchronized void registerTool(String name, String description, Method method,
                                        Object instance, boolean async, long timeout, int priority) {
        if (tools.containsKey(name)) {
            throw new ClaudeCodeException("工具名称冲突: " + name);
        }

        ToolDefinition toolDefinition = new ToolDefinition(
                name, description, method, instance, async, timeout, priority
        );

        tools.put(name, toolDefinition);
        logger.info("手动注册工具: {}", name);
    }

    /**
     * 注销工具
     */
    public synchronized boolean unregisterTool(String name) {
        ToolDefinition removed = tools.remove(name);
        if (removed != null) {
            logger.info("注销工具: {}", name);
            return true;
        }
        return false;
    }

    /**
     * 注销实例的所有工具
     */
    public synchronized void unregisterInstance(Object instance) {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ToolDefinition> entry : tools.entrySet()) {
            if (entry.getValue().getInstance() == instance) {
                toRemove.add(entry.getKey());
            }
        }

        for (String toolName : toRemove) {
            tools.remove(toolName);
            logger.debug("注销工具: {}", toolName);
        }

        registeredInstances.remove(instance);

        if (!toRemove.isEmpty()) {
            logger.info("从实例 {} 注销了 {} 个工具",
                    instance.getClass().getSimpleName(), toRemove.size());
        }
    }

    /**
     * 获取所有工具定义
     */
    public List<ToolDefinition> getToolDefinitions() {
        return new ArrayList<>(tools.values())
                .stream()
                .sorted(Comparator.comparingInt(ToolDefinition::getPriority))
                .toList();
    }

    /**
     * 获取工具定义
     */
    public Optional<ToolDefinition> getToolDefinition(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * 获取工具名称列表
     */
    public Set<String> getToolNames() {
        return new HashSet<>(tools.keySet());
    }

    /**
     * 执行工具
     */
    public CompletableFuture<ToolExecutionResult> executeTool(String toolName, JsonNode arguments) {
        ToolDefinition toolDefinition = tools.get(toolName);
        if (toolDefinition == null) {
            CompletableFuture<ToolExecutionResult> future = new CompletableFuture<>();
            future.complete(ToolExecutionResult.error("工具不存在: " + toolName).build());
            return future;
        }

        logger.debug("执行工具: {} with args: {}", toolName, arguments);
        return toolExecutor.executeAsync(toolDefinition, arguments);
    }

    /**
     * 获取工具统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTools", tools.size());
        stats.put("registeredInstances", registeredInstances.size());

        Map<String, Long> toolsByType = tools.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        tool -> tool.isAsync() ? "async" : "sync",
                        java.util.stream.Collectors.counting()
                ));
        stats.put("toolsByType", toolsByType);

        return stats;
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        logger.info("正在关闭MCP服务器...");
        toolExecutor.shutdown();
        tools.clear();
        registeredInstances.clear();
        logger.info("MCP服务器已关闭");
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        return true; // 简单实现，可以扩展
    }

    @Override
    public String toString() {
        return "MCPServer{" +
                "toolCount=" + tools.size() +
                ", instanceCount=" + registeredInstances.size() +
                '}';
    }
}