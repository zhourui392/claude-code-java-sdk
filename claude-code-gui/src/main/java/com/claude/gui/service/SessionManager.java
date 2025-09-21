package com.claude.gui.service;

import com.claude.gui.model.ChatSession;
import com.claude.gui.model.Message;
import com.claude.gui.model.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会话管理服务类
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class SessionManager {

    private List<ChatSession> sessions;
    private ChatSession currentSession;

    public SessionManager() {
        this.sessions = new ArrayList<>();
        this.currentSession = null;
    }

    /**
     * 创建新会话
     */
    public ChatSession createNewSession() {
        return createNewSession(null);
    }

    /**
     * 创建新会话，指定名称
     */
    public ChatSession createNewSession(String name) {
        ChatSession session = name != null ? new ChatSession(name) : new ChatSession();
        sessions.add(session);
        return session;
    }

    /**
     * 删除会话
     */
    public boolean deleteSession(String sessionId) {
        Optional<ChatSession> sessionOpt = findSessionById(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();

            // 如果删除的是当前会话，需要切换到其他会话
            if (session.equals(currentSession)) {
                currentSession = null;
            }

            return sessions.remove(session);
        }
        return false;
    }

    /**
     * 切换到指定会话
     */
    public boolean switchToSession(String sessionId) {
        Optional<ChatSession> sessionOpt = findSessionById(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();

            // 停用之前的会话
            if (currentSession != null) {
                currentSession.setActive(false);
            }

            // 激活新会话
            currentSession = session;
            currentSession.setActive(true);
            return true;
        }
        return false;
    }

    /**
     * 向当前会话添加消息
     */
    public void addMessageToCurrentSession(String sender, String content, MessageType type) {
        if (currentSession != null) {
            currentSession.addMessage(sender, content, type);
        }
    }

    /**
     * 重命名会话
     */
    public boolean renameSession(String sessionId, String newName) {
        Optional<ChatSession> sessionOpt = findSessionById(sessionId);
        if (sessionOpt.isPresent()) {
            sessionOpt.get().setName(newName);
            return true;
        }
        return false;
    }

    /**
     * 获取所有会话，按最后活跃时间排序
     */
    public List<ChatSession> getAllSessions() {
        return sessions.stream()
                .sorted((a, b) -> b.getLastActiveTime().compareTo(a.getLastActiveTime()))
                .collect(Collectors.toList());
    }

    /**
     * 根据ID查找会话
     */
    public Optional<ChatSession> findSessionById(String sessionId) {
        return sessions.stream()
                .filter(session -> session.getId().equals(sessionId))
                .findFirst();
    }

    /**
     * 获取当前会话
     */
    public ChatSession getCurrentSession() {
        return currentSession;
    }

    /**
     * 检查是否有活跃会话
     */
    public boolean hasActiveSession() {
        return currentSession != null;
    }

    /**
     * 获取会话总数
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 清空所有会话
     */
    public void clearAllSessions() {
        sessions.clear();
        currentSession = null;
    }

    /**
     * 获取当前会话的所有消息
     */
    public List<Message> getCurrentSessionMessages() {
        return currentSession != null ? currentSession.getMessages() : new ArrayList<>();
    }

    /**
     * 检查会话名称是否存在
     */
    public boolean isSessionNameExists(String name) {
        return sessions.stream()
                .anyMatch(session -> session.getName().equals(name));
    }
}