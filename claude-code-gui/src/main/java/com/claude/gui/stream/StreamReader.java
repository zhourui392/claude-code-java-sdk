package com.claude.gui.stream;

import com.claude.gui.callback.MessageCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 流式读取器类，用于在后台线程中读取Claude CLI的输出流
 * 并通过回调接口实时通知GUI界面更新
 *
 * @author Claude Code GUI Team
 * @version 1.0.0
 */
public class StreamReader extends Thread {

    private final BufferedReader reader;
    private final MessageCallback callback;
    private final String streamType;
    private volatile boolean running;
    private volatile boolean stopRequested;

    /**
     * 构造函数
     *
     * @param inputStream 要读取的输入流
     * @param callback    消息回调接口
     * @param streamType  流类型标识（用于日志和调试）
     */
    public StreamReader(InputStream inputStream, MessageCallback callback, String streamType) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        this.callback = callback;
        this.streamType = streamType;
        this.running = false;
        this.stopRequested = false;
        this.setDaemon(true);
        this.setName("StreamReader-" + streamType);
    }

    /**
     * 线程主执行方法
     * 持续读取流内容直到流结束或收到停止请求
     */
    @Override
    public void run() {
        running = true;
        String line;

        try {
            while (!stopRequested && (line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    // 通过回调接口发送消息到GUI线程
                    if (callback != null) {
                        if ("error".equals(streamType)) {
                            // 检查是否是真正的错误信息，还是只是提示信息
                            if (isActualError(line)) {
                                callback.onError(line);
                            } else {
                                // 将提示信息作为普通消息处理
                                callback.onMessageReceived("[提示] " + line);
                            }
                        } else {
                            callback.onMessageReceived(line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (!stopRequested && callback != null) {
                callback.onError("读取" + streamType + "流时发生错误: " + e.getMessage());
            }
        } finally {
            closeReader();
            running = false;
        }
    }

    /**
     * 停止读取流
     * 设置停止标志并中断线程
     */
    public void stopReading() {
        stopRequested = true;
        this.interrupt();
        closeReader();
    }

    /**
     * 检查读取器是否正在运行
     *
     * @return 如果正在运行返回true，否则返回false
     */
    public boolean isRunning() {
        return running && !stopRequested;
    }

    /**
     * 安全关闭读取器
     */
    private void closeReader() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // 忽略关闭时的异常
                System.err.println("关闭" + streamType + "流读取器时发生错误: " + e.getMessage());
            }
        }
    }

    /**
     * 获取流类型
     *
     * @return 流类型标识
     */
    public String getStreamType() {
        return streamType;
    }

    /**
     * 判断消息是否是真正的错误信息
     *
     * @param message 消息内容
     * @return 如果是错误信息返回true，否则返回false
     */
    private boolean isActualError(String message) {
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase().trim();

        // 明确的错误关键词
        String[] errorKeywords = {
            "error:",
            "exception:",
            "failed:",
            "failure:",
            "cannot",
            "unable to",
            "permission denied",
            "not found",
            "access denied",
            "invalid",
            "syntax error",
            "compilation error",
            "runtime error"
        };

        for (String keyword : errorKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }

        // 提示信息关键词（这些不是错误）
        String[] tipKeywords = {
            "tip:",
            "hint:",
            "suggestion:",
            "note:",
            "info:",
            "you can launch",
            "welcome",
            "starting",
            "initialized"
        };

        for (String keyword : tipKeywords) {
            if (lowerMessage.contains(keyword)) {
                return false;
            }
        }

        // 如果包含"warning"，判断为警告而不是错误
        if (lowerMessage.contains("warning")) {
            return false;
        }

        // 默认情况下，如果无法确定，则不视为错误
        return false;
    }
}