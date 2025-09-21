package com.anthropic.claude.client;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 持续对话客户端
 * 支持双向交互、会话状态管理和消息接收
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ClaudeSDKClient {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeSDKClient.class);

    private final ClaudeCodeOptions options;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private final AtomicReference<String> sessionId = new AtomicReference<>();

    private final PublishSubject<Message> messageSubject = PublishSubject.create();
    private final SessionManager sessionManager;
    private final MessageReceiver messageReceiver;
    private final InterruptHandler interruptHandler;

    public ClaudeSDKClient(ClaudeCodeOptions options) {
        this.options = options;
        this.sessionManager = new SessionManager(options);
        this.messageReceiver = new MessageReceiver(messageSubject);
        this.interruptHandler = new InterruptHandler();

        logger.info("双向交互客户端已创建");
    }

    /**
     * 连接到Claude服务
     *
     * @return 连接操作的CompletableFuture
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            if (connected.get()) {
                logger.warn("客户端已连接，跳过连接操作");
                return;
            }

            try {
                logger.info("正在连接到Claude服务...");

                // 初始化会话
                String newSessionId = sessionManager.createSession();
                sessionId.set(newSessionId);

                // 启动消息接收器
                messageReceiver.start();

                // 重置中断状态
                interrupted.set(false);

                connected.set(true);
                logger.info("成功连接到Claude服务，会话ID: {}", newSessionId);

                // 发送连接成功消息
                Message connectMessage = new Message(MessageType.SYSTEM,
                    "连接已建立，会话ID: " + newSessionId);
                messageSubject.onNext(connectMessage);

            } catch (Exception e) {
                logger.error("连接失败", e);
                throw new ClaudeCodeException("连接到Claude服务失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 断开连接
     *
     * @return 断开操作的CompletableFuture
     */
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            if (!connected.get()) {
                logger.warn("客户端未连接，跳过断开操作");
                return;
            }

            try {
                logger.info("正在断开Claude服务连接...");

                // 停止消息接收器
                messageReceiver.stop();

                // 清理会话
                sessionManager.cleanupSession(sessionId.get());
                sessionId.set(null);

                connected.set(false);
                logger.info("已断开与Claude服务的连接");

                // 发送断开连接消息
                Message disconnectMessage = new Message(MessageType.SYSTEM, "连接已断开");
                messageSubject.onNext(disconnectMessage);

            } catch (Exception e) {
                logger.error("断开连接时出错", e);
                throw new ClaudeCodeException("断开连接失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 接收消息流
     *
     * @return 消息Observable流
     */
    public Observable<Message> receiveMessages() {
        if (!connected.get()) {
            throw new ClaudeCodeException("客户端未连接，无法接收消息");
        }

        logger.debug("开始接收消息流");
        return messageSubject;
    }

    /**
     * 接收单个响应消息
     *
     * @return 响应消息的CompletableFuture
     */
    public CompletableFuture<Message> receiveResponse() {
        return receiveResponse(MessageType.ASSISTANT);
    }

    /**
     * 接收指定类型的响应消息
     *
     * @param messageType 期望的消息类型
     * @return 响应消息的CompletableFuture
     */
    public CompletableFuture<Message> receiveResponse(MessageType messageType) {
        if (!connected.get()) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            future.completeExceptionally(new ClaudeCodeException("客户端未连接"));
            return future;
        }

        CompletableFuture<Message> future = new CompletableFuture<>();

        // 订阅消息流，等待指定类型的消息
        receiveMessages()
            .filter(msg -> msg.getType() == messageType)
            .take(1)
            .subscribe(
                message -> {
                    logger.debug("接收到响应消息: {}", message.getType());
                    future.complete(message);
                },
                error -> {
                    logger.error("接收响应消息失败", error);
                    future.completeExceptionally(error);
                }
            );

        return future;
    }

    /**
     * 中断当前查询
     *
     * @return 中断操作的CompletableFuture
     */
    public CompletableFuture<Void> interrupt() {
        return CompletableFuture.runAsync(() -> {
            if (!connected.get()) {
                logger.warn("客户端未连接，无法中断");
                return;
            }

            try {
                logger.info("正在中断当前查询...");
                interrupted.set(true);

                // 执行中断操作
                interruptHandler.performInterrupt(sessionId.get());

                // 发送中断消息
                Message interruptMessage = new Message(MessageType.SYSTEM, "查询已中断");
                messageSubject.onNext(interruptMessage);

                logger.info("查询中断完成");

            } catch (Exception e) {
                logger.error("中断操作失败", e);
                throw new ClaudeCodeException("中断失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 检查是否已连接
     *
     * @return 连接状态
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 检查是否已中断
     *
     * @return 中断状态
     */
    public boolean isInterrupted() {
        return interrupted.get();
    }

    /**
     * 获取当前会话ID
     *
     * @return 会话ID，未连接时返回null
     */
    public String getSessionId() {
        return sessionId.get();
    }

    /**
     * 获取会话状态信息
     *
     * @return 会话状态
     */
    public SessionState getSessionState() {
        String currentSessionId = sessionId.get();
        if (currentSessionId == null) {
            return new SessionState(false, null, 0);
        }

        return sessionManager.getSessionState(currentSessionId);
    }

    /**
     * 重置中断状态
     */
    public void resetInterruptState() {
        interrupted.set(false);
        logger.debug("中断状态已重置");
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        try {
            if (connected.get()) {
                disconnect().get();
            }

            messageSubject.onComplete();
            sessionManager.shutdown();
            messageReceiver.shutdown();
            interruptHandler.shutdown();

            logger.info("双向交互客户端已关闭");

        } catch (Exception e) {
            logger.error("关闭客户端时出错", e);
        }
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    public boolean isHealthy() {
        return connected.get() &&
               sessionId.get() != null &&
               sessionManager.isHealthy();
    }

    /**
     * 会话状态数据类
     */
    public static class SessionState {
        private final boolean active;
        private final String sessionId;
        private final long messageCount;

        public SessionState(boolean active, String sessionId, long messageCount) {
            this.active = active;
            this.sessionId = sessionId;
            this.messageCount = messageCount;
        }

        public boolean isActive() { return active; }
        public String getSessionId() { return sessionId; }
        public long getMessageCount() { return messageCount; }

        @Override
        public String toString() {
            return String.format("SessionState{active=%s, sessionId='%s', messageCount=%d}",
                    active, sessionId, messageCount);
        }
    }
}