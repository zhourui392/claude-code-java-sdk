package com.claude.gui.callback;

/**
 * 消息回调接口，用于处理Claude CLI执行过程中的各种消息和事件
 *
 * @author Claude Code GUI Team
 * @version 1.0.0
 */
public interface MessageCallback {

    /**
     * 当接收到Claude CLI输出消息时调用
     *
     * @param message 接收到的消息内容
     */
    void onMessageReceived(String message);

    /**
     * 当发生错误时调用
     *
     * @param error 错误信息
     */
    void onError(String error);

    /**
     * 当Claude CLI进程完成时调用
     */
    void onProcessFinished();

    /**
     * 当进程启动时调用
     */
    void onProcessStarted();

    /**
     * 当连接状态改变时调用
     *
     * @param connected 是否已连接
     */
    void onConnectionStateChanged(boolean connected);
}