package com.anthropic.claude.client;

import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageType;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消息接收器
 * 负责接收和分发Claude服务的消息
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class MessageReceiver {
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    private final PublishSubject<Message> messageSubject;
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "message-receiver");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MessageReceiver(PublishSubject<Message> messageSubject) {
        this.messageSubject = messageSubject;
        logger.debug("消息接收器已创建");
    }

    /**
     * 启动消息接收器
     */
    public void start() {
        if (running.get()) {
            logger.warn("消息接收器已在运行");
            return;
        }

        running.set(true);
        executorService.submit(this::receiveLoop);
        logger.info("消息接收器已启动");
    }

    /**
     * 停止消息接收器
     */
    public void stop() {
        if (!running.get()) {
            logger.warn("消息接收器未在运行");
            return;
        }

        running.set(false);
        logger.info("消息接收器已停止");
    }

    /**
     * 添加消息到接收队列
     *
     * @param message 要添加的消息
     */
    public void addMessage(Message message) {
        if (running.get()) {
            try {
                messageQueue.offer(message);
                logger.debug("添加消息到接收队列: {}", message.getType());
            } catch (Exception e) {
                logger.error("添加消息到队列失败", e);
            }
        }
    }

    /**
     * 模拟接收到系统消息
     *
     * @param content 消息内容
     */
    public void receiveSystemMessage(String content) {
        Message message = new Message(MessageType.SYSTEM, content);
        addMessage(message);
    }

    /**
     * 模拟接收到助手回复
     *
     * @param content 回复内容
     */
    public void receiveAssistantMessage(String content) {
        Message message = new Message(MessageType.ASSISTANT, content);
        addMessage(message);
    }

    /**
     * 模拟接收到错误消息
     *
     * @param error 错误信息
     */
    public void receiveErrorMessage(String error) {
        Message message = new Message(MessageType.ERROR, error);
        addMessage(message);
    }

    /**
     * 获取队列中的消息数量
     *
     * @return 消息数量
     */
    public int getQueueSize() {
        return messageQueue.size();
    }

    /**
     * 检查是否正在运行
     *
     * @return 运行状态
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 关闭消息接收器
     */
    public void shutdown() {
        stop();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        messageQueue.clear();
        logger.info("消息接收器已关闭");
    }

    /**
     * 消息接收循环
     */
    private void receiveLoop() {
        logger.debug("消息接收循环已启动");

        while (running.get()) {
            try {
                // 从队列中取消息，带超时
                Message message = messageQueue.poll(1000, java.util.concurrent.TimeUnit.MILLISECONDS);

                if (message != null) {
                    // 分发消息
                    messageSubject.onNext(message);
                    logger.debug("分发消息: {} - {}", message.getType(),
                            message.getContent().length() > 50 ?
                            message.getContent().substring(0, 50) + "..." :
                            message.getContent());
                }

                // 模拟接收Claude服务的消息
                simulateIncomingMessages();

            } catch (InterruptedException e) {
                logger.debug("消息接收循环被中断");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("消息接收循环出错", e);
                // 继续运行，不要因为单个错误而停止
            }
        }

        logger.debug("消息接收循环已结束");
    }

    /**
     * 模拟接收传入消息
     * 在实际实现中，这里会连接到真正的Claude服务
     */
    private void simulateIncomingMessages() {
        // 这是一个模拟实现
        // 在真实实现中，这里会从Claude CLI进程或API接收消息

        // 定期发送心跳消息
        if (System.currentTimeMillis() % 30000 < 1000) { // 每30秒大约发送一次
            receiveSystemMessage("心跳: " + java.time.LocalDateTime.now());
        }
    }

    /**
     * 处理特殊消息类型的过滤
     *
     * @param message 消息
     * @return 是否应该转发消息
     */
    private boolean shouldForwardMessage(Message message) {
        // 可以在这里实现消息过滤逻辑
        // 例如：过滤掉某些调试消息
        if (message.getType() == MessageType.DEBUG) {
            return logger.isDebugEnabled();
        }

        return true;
    }
}