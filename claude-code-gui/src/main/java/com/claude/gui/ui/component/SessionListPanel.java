package com.claude.gui.ui.component;

import com.claude.gui.model.ChatSession;
import com.claude.gui.service.SessionManager;
import com.claude.gui.ui.ThemeConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 会话列表面板
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class SessionListPanel extends JPanel {

    private SessionManager sessionManager;
    private JList<ChatSession> sessionList;
    private DefaultListModel<ChatSession> listModel;
    private JButton newSessionButton;
    private JButton deleteSessionButton;
    private JButton renameSessionButton;
    private SessionSelectionListener selectionListener;

    public interface SessionSelectionListener {
        void onSessionSelected(ChatSession session);
        void onSessionDeleted(ChatSession session);
        void onNewSessionCreated(ChatSession session);
    }

    public SessionListPanel(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refreshSessionList();
    }

    private void initializeComponents() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ThemeConfig.BACKGROUND_COLOR);

        // 创建列表模型和列表
        listModel = new DefaultListModel<>();
        sessionList = new JList<>(listModel);
        sessionList.setCellRenderer(new SessionListCellRenderer());
        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionList.setFixedCellHeight(80);

        // 创建按钮
        newSessionButton = new JButton("新建会话");
        ThemeConfig.styleButton(newSessionButton, ThemeConfig.ButtonType.PRIMARY);

        deleteSessionButton = new JButton("删除");
        ThemeConfig.styleButton(deleteSessionButton, ThemeConfig.ButtonType.DANGER);
        deleteSessionButton.setEnabled(false);

        renameSessionButton = new JButton("重命名");
        ThemeConfig.styleButton(renameSessionButton, ThemeConfig.ButtonType.SECONDARY);
        renameSessionButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(0, 10));

        // 顶部：标题和新建按钮
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("会话列表");
        titleLabel.setFont(ThemeConfig.createBoldFont(16));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(newSessionButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 中间：会话列表
        JScrollPane scrollPane = new JScrollPane(sessionList);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // 底部：操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(renameSessionButton);
        buttonPanel.add(deleteSessionButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        // 会话列表选择事件 - 只更新按钮状态，不触发会话切换
        sessionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChatSession selectedSession = sessionList.getSelectedValue();
                boolean hasSelection = selectedSession != null;

                deleteSessionButton.setEnabled(hasSelection);
                renameSessionButton.setEnabled(hasSelection);

                // 注意：这里不调用 onSessionSelected，避免频繁的会话切换
                // 会话切换只在双击或其他明确的用户操作时进行
            }
        });

        // 双击会话项切换会话
        sessionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ChatSession selectedSession = sessionList.getSelectedValue();
                    if (selectedSession != null) {
                        switchToSession(selectedSession);
                    }
                }
            }
        });

        // 新建会话按钮
        newSessionButton.addActionListener(e -> createNewSession());

        // 删除会话按钮
        deleteSessionButton.addActionListener(e -> deleteSelectedSession());

        // 重命名会话按钮
        renameSessionButton.addActionListener(e -> renameSelectedSession());
    }

    /**
     * 创建新会话
     */
    private void createNewSession() {
        String name = JOptionPane.showInputDialog(
                this,
                "请输入会话名称:",
                "新建会话",
                JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();

            // 检查名称是否已存在
            if (sessionManager.isSessionNameExists(name)) {
                JOptionPane.showMessageDialog(
                        this,
                        "会话名称已存在，请使用其他名称。",
                        "创建失败",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            ChatSession newSession = sessionManager.createNewSession(name);
            refreshSessionList();

            // 选中新创建的会话
            sessionList.setSelectedValue(newSession, true);

            if (selectionListener != null) {
                selectionListener.onNewSessionCreated(newSession);
            }
        }
    }

    /**
     * 删除选中的会话
     */
    private void deleteSelectedSession() {
        ChatSession selectedSession = sessionList.getSelectedValue();
        if (selectedSession == null) return;

        int result = JOptionPane.showConfirmDialog(
                this,
                "确定要删除会话 \"" + selectedSession.getName() + "\" 吗？\n此操作无法撤销。",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            boolean deleted = sessionManager.deleteSession(selectedSession.getId());
            if (deleted) {
                refreshSessionList();
                if (selectionListener != null) {
                    selectionListener.onSessionDeleted(selectedSession);
                }
            }
        }
    }

    /**
     * 重命名选中的会话
     */
    private void renameSelectedSession() {
        ChatSession selectedSession = sessionList.getSelectedValue();
        if (selectedSession == null) return;

        String newName = JOptionPane.showInputDialog(
                this,
                "请输入新的会话名称:",
                "重命名会话",
                JOptionPane.PLAIN_MESSAGE
        );

        if (newName != null && !newName.trim().isEmpty()) {
            newName = newName.trim();

            // 检查名称是否已存在（不包括当前会话）
            if (!newName.equals(selectedSession.getName()) && sessionManager.isSessionNameExists(newName)) {
                JOptionPane.showMessageDialog(
                        this,
                        "会话名称已存在，请使用其他名称。",
                        "重命名失败",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            boolean renamed = sessionManager.renameSession(selectedSession.getId(), newName);
            if (renamed) {
                refreshSessionList();
                sessionList.setSelectedValue(selectedSession, true);
            }
        }
    }

    /**
     * 切换到指定会话
     */
    private void switchToSession(ChatSession session) {
        // 直接通知监听器进行会话切换，避免重复调用sessionManager.switchToSession
        if (selectionListener != null) {
            selectionListener.onSessionSelected(session);
        }
        refreshSessionList();
    }

    /**
     * 刷新会话列表
     */
    public void refreshSessionList() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            List<ChatSession> sessions = sessionManager.getAllSessions();
            for (ChatSession session : sessions) {
                listModel.addElement(session);
            }

            // 选中当前活跃会话
            ChatSession currentSession = sessionManager.getCurrentSession();
            if (currentSession != null) {
                sessionList.setSelectedValue(currentSession, true);
            }

            sessionList.repaint();
        });
    }

    /**
     * 设置会话选择监听器
     */
    public void setSelectionListener(SessionSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * 获取当前选中的会话
     */
    public ChatSession getSelectedSession() {
        return sessionList.getSelectedValue();
    }
}