package com.anthropic.claude.strategy;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.query.QueryRequest;
import io.reactivex.rxjava3.core.Observable;

import java.util.stream.Stream;

/**
 * CLI执行策略接口
 *
 * 定义两种执行模式的统一接口：
 * 1. 批处理模式：每次调用启动CLI，读取输出后进程结束
 * 2. PTY交互模式：维护常驻会话，向stdin写入，按行监听stdout
 *
 * @author Claude Code SDK
 */
public interface CliExecutionStrategy {

    /**
     * 启动策略（如果需要初始化）
     *
     * @throws ClaudeCodeException 启动失败时抛出
     */
    void start() throws ClaudeCodeException;

    /**
     * 关闭策略，释放资源
     *
     * @throws ClaudeCodeException 关闭失败时抛出
     */
    void shutdown() throws ClaudeCodeException;

    /**
     * 执行查询请求（同步模式）
     *
     * @param request 查询请求
     * @return 消息流
     * @throws ClaudeCodeException 执行失败时抛出
     */
    Stream<Message> execute(QueryRequest request) throws ClaudeCodeException;

    /**
     * 执行查询请求（流式模式）
     *
     * @param request 查询请求
     * @return 响应式消息流
     * @throws ClaudeCodeException 执行失败时抛出
     */
    Observable<Message> executeStream(QueryRequest request) throws ClaudeCodeException;

    /**
     * 检查策略是否可用
     *
     * @return 如果策略可用返回true
     */
    boolean isAvailable();

    /**
     * 获取策略类型
     *
     * @return CLI模式
     */
    String getStrategyType();

    /**
     * 是否支持会话保持
     *
     * @return 支持会话保持返回true
     */
    boolean supportsSessionPersistence();
}