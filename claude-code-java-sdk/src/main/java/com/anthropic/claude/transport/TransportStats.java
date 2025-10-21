package com.anthropic.claude.transport;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 传输层统计信息
 *
 * <p>记录传输层的运行统计数据，用于监控和调试。</p>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class TransportStats {

    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    /**
     * 增加发送消息计数
     */
    public void incrementMessagesSent() {
        messagesSent.incrementAndGet();
    }

    /**
     * 增加接收消息计数
     */
    public void incrementMessagesReceived() {
        messagesReceived.incrementAndGet();
    }

    /**
     * 增加发送字节数
     *
     * @param bytes 字节数
     */
    public void addBytesSent(long bytes) {
        bytesSent.addAndGet(bytes);
    }

    /**
     * 增加接收字节数
     *
     * @param bytes 字节数
     */
    public void addBytesReceived(long bytes) {
        bytesReceived.addAndGet(bytes);
    }

    /**
     * 增加错误计数
     */
    public void incrementErrors() {
        errors.incrementAndGet();
    }

    /**
     * 获取发送的消息数
     *
     * @return 消息数
     */
    public long getMessagesSent() {
        return messagesSent.get();
    }

    /**
     * 获取接收的消息数
     *
     * @return 消息数
     */
    public long getMessagesReceived() {
        return messagesReceived.get();
    }

    /**
     * 获取发送的字节数
     *
     * @return 字节数
     */
    public long getBytesSent() {
        return bytesSent.get();
    }

    /**
     * 获取接收的字节数
     *
     * @return 字节数
     */
    public long getBytesReceived() {
        return bytesReceived.get();
    }

    /**
     * 获取错误数
     *
     * @return 错误数
     */
    public long getErrors() {
        return errors.get();
    }

    /**
     * 获取运行时间（毫秒）
     *
     * @return 运行时间
     */
    public long getUptimeMillis() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取开始时间（毫秒时间戳）
     *
     * @return 开始时间
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        messagesSent.set(0);
        messagesReceived.set(0);
        bytesSent.set(0);
        bytesReceived.set(0);
        errors.set(0);
    }

    @Override
    public String toString() {
        return String.format(
            "TransportStats{sent=%d, received=%d, bytesSent=%d, bytesReceived=%d, " +
            "errors=%d, uptime=%dms}",
            messagesSent.get(), messagesReceived.get(),
            bytesSent.get(), bytesReceived.get(),
            errors.get(), getUptimeMillis()
        );
    }
}
