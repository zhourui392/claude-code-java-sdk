package com.anthropic.claude.transport;

import io.reactivex.rxjava3.core.Observable;

/**
 * Claude 传输层接口
 *
 * <p>定义 Claude Agent SDK 与 Claude 服务通信的抽象接口。</p>
 *
 * <p>实现此接口可以自定义通信方式，例如：</p>
 * <ul>
 *   <li>CLI 进程通信 ({@link ProcessTransport})</li>
 *   <li>HTTP API 通信</li>
 *   <li>WebSocket 通信</li>
 *   <li>自定义协议</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用自定义传输层
 * public class MyCustomTransport implements ClaudeTransport {
 *     @Override
 *     public void send(String message) throws TransportException {
 *         // 实现发送逻辑
 *     }
 *
 *     @Override
 *     public Observable<String> receive() throws TransportException {
 *         // 实现接收逻辑
 *         return Observable.create(emitter -> {
 *             // 流式接收消息
 *         });
 *     }
 *
 *     // ... 其他方法
 * }
 *
 * // 使用自定义传输层
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .transport(new MyCustomTransport())
 *     .build();
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public interface ClaudeTransport extends AutoCloseable {

    /**
     * 发送消息到 Claude
     *
     * @param message 消息内容
     * @throws TransportException 传输错误
     */
    void send(String message) throws TransportException;

    /**
     * 接收 Claude 响应流
     *
     * <p>返回一个 Observable 流，用于接收 Claude 的响应。
     * 实现应该支持流式响应，即逐行或逐块返回数据。</p>
     *
     * @return 响应流的 Observable
     * @throws TransportException 传输错误
     */
    Observable<String> receive() throws TransportException;

    /**
     * 检查连接状态
     *
     * @return 如果已连接返回 true，否则返回 false
     */
    boolean isConnected();

    /**
     * 关闭传输层并释放资源
     *
     * <p>此方法应该：</p>
     * <ul>
     *   <li>关闭所有打开的连接</li>
     *   <li>清理临时资源</li>
     *   <li>停止后台线程</li>
     * </ul>
     *
     * @throws TransportException 关闭时发生错误
     */
    @Override
    void close() throws TransportException;

    /**
     * 获取传输层类型
     *
     * @return 传输类型
     */
    TransportType getType();

    /**
     * 获取传输统计信息（可选）
     *
     * <p>默认实现返回一个空的统计对象。
     * 子类可以重写此方法提供详细的统计信息。</p>
     *
     * @return 传输统计信息
     */
    default TransportStats getStats() {
        return new TransportStats();
    }

    /**
     * 重置传输层（可选）
     *
     * <p>默认实现不做任何操作。
     * 子类可以重写此方法实现重置逻辑。</p>
     *
     * @throws TransportException 重置失败
     */
    default void reset() throws TransportException {
        // 默认不做任何操作
    }
}
