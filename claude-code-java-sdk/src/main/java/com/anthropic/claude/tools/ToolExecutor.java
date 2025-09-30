package com.anthropic.claude.tools;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.*;

/**
 * 工具执行引擎
 * 负责调用工具方法并处理结果
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ToolExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutor.class);

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public ToolExecutor() {
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "tool-executor-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 执行工具方法
     *
     * @param toolDefinition 工具定义
     * @param arguments 参数JSON
     * @return 执行结果
     */
    public CompletableFuture<ToolExecutionResult> executeAsync(ToolDefinition toolDefinition,
                                                              JsonNode arguments) {
        if (toolDefinition.isAsync()) {
            return executeAsyncTool(toolDefinition, arguments);
        } else {
            return CompletableFuture.supplyAsync(() -> executeSyncTool(toolDefinition, arguments),
                    executorService);
        }
    }

    /**
     * 同步执行工具
     */
    private ToolExecutionResult executeSyncTool(ToolDefinition toolDefinition, JsonNode arguments) {
        long startTime = System.currentTimeMillis();

        try {
            Object result = invokeTool(toolDefinition, arguments);
            long executionTime = System.currentTimeMillis() - startTime;

            return ToolExecutionResult.success(result)
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("工具执行失败: {}", toolDefinition.getName(), e);

            return ToolExecutionResult.error(e)
                    .executionTime(executionTime)
                    .build();
        }
    }

    /**
     * 异步执行工具
     */
    private CompletableFuture<ToolExecutionResult> executeAsyncTool(ToolDefinition toolDefinition,
                                                                   JsonNode arguments) {
        long startTime = System.currentTimeMillis();

        CompletableFuture<ToolExecutionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object result = invokeTool(toolDefinition, arguments);
                long executionTime = System.currentTimeMillis() - startTime;

                return ToolExecutionResult.success(result)
                        .executionTime(executionTime)
                        .build();

            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                logger.error("异步工具执行失败: {}", toolDefinition.getName(), e);

                return ToolExecutionResult.error(e)
                        .executionTime(executionTime)
                        .build();
            }
        }, executorService);

        // JDK 8 兼容的超时处理
        CompletableFuture<ToolExecutionResult> timeoutFuture = new CompletableFuture<>();

        // 创建超时任务
        ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeoutTask = timeoutExecutor.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                timeoutFuture.complete(ToolExecutionResult.error("工具执行超时")
                        .executionTime(toolDefinition.getTimeout())
                        .build());
            }
        }, toolDefinition.getTimeout(), TimeUnit.MILLISECONDS);

        // 原future完成时的处理
        future.whenComplete((result, throwable) -> {
            timeoutTask.cancel(false); // 取消超时任务
            timeoutExecutor.shutdown();

            if (throwable != null) {
                if (!timeoutFuture.isDone()) {
                    logger.error("工具执行异常: {}", toolDefinition.getName(), throwable);
                    timeoutFuture.complete(ToolExecutionResult.error((Throwable)(throwable.getCause() != null ?
                            throwable.getCause() : throwable))
                            .build());
                }
            } else {
                if (!timeoutFuture.isDone()) {
                    timeoutFuture.complete(result);
                }
            }
        });

        return timeoutFuture;
    }

    /**
     * 调用工具方法
     */
    private Object invokeTool(ToolDefinition toolDefinition, JsonNode arguments) throws Exception {
        Method method = toolDefinition.getMethod();
        Object instance = toolDefinition.getInstance();

        // 准备参数
        Object[] args = prepareArguments(method, arguments);

        // 调用方法
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    /**
     * 准备方法参数
     */
    private Object[] prepareArguments(Method method, JsonNode arguments) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            // 首先尝试获取@Param注解中的参数名
            String paramName = getParameterName(param, i);

            JsonNode argNode = arguments.get(paramName);

            if (argNode == null || argNode.isNull()) {
                // 检查是否为必需参数
                if (isRequiredParameter(param, paramType)) {
                    throw new ClaudeCodeException("必需参数缺失: " + paramName);
                }
                args[i] = getDefaultValue(paramType);
            } else {
                args[i] = objectMapper.treeToValue(argNode, paramType);
            }
        }

        return args;
    }

    /**
     * 获取参数名称
     */
    private String getParameterName(Parameter param, int index) {
        // 首先检查@Param注解
        Param paramAnnotation = param.getAnnotation(Param.class);
        if (paramAnnotation != null) {
            return paramAnnotation.value();
        }

        // 尝试获取实际参数名（需要编译时保留参数信息）
        if (param.isNamePresent()) {
            return param.getName();
        }

        // 如果都没有，使用默认的参数名格式
        return "arg" + index;
    }

    /**
     * 检查是否为必需参数
     */
    private boolean isRequiredParameter(Parameter param, Class<?> paramType) {
        Param paramAnnotation = param.getAnnotation(Param.class);
        if (paramAnnotation != null) {
            return paramAnnotation.required();
        }

        // 基本类型通常是必需的
        return paramType.isPrimitive();
    }

    /**
     * 获取参数的默认值
     */
    private Object getDefaultValue(Class<?> paramType) {
        if (paramType.isPrimitive()) {
            if (paramType == boolean.class) return false;
            if (paramType == byte.class) return (byte) 0;
            if (paramType == short.class) return (short) 0;
            if (paramType == int.class) return 0;
            if (paramType == long.class) return 0L;
            if (paramType == float.class) return 0.0f;
            if (paramType == double.class) return 0.0d;
            if (paramType == char.class) return '\0';
        }
        return null;
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}