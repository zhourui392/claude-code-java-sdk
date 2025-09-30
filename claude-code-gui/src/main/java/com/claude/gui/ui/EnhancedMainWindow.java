package com.claude.gui.ui;

import com.claude.gui.callback.MessageCallback;
import com.claude.gui.executor.SessionAwareClaudeExecutor;
import com.claude.gui.model.ChatSession;
import com.claude.gui.model.MessageType;
import com.claude.gui.service.SessionManager;
import com.claude.gui.ui.component.SessionListPanel;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 增强的主窗口类，支持会话管理和--resume功能
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class EnhancedMainWindow extends JFrame implements MessageCallback, SessionListPanel.SessionSelectionListener {

    private static final int MAX_HISTORY_LINES = 1000;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // GUI组件
    private JTextPane conversationArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JLabel statusLabel;
    private JScrollPane conversationScrollPane;
    private SessionListPanel sessionListPanel;
    private JSplitPane mainSplitPane;

    // 业务组件
    private SessionAwareClaudeExecutor cliExecutor;
    private SessionManager sessionManager;

    // 样式
    private StyledDocument document;
    private Style userStyle;
    private Style claudeStyle;
    private Style errorStyle;
    private Style systemStyle;

    /**
     * 构造函数
     */
    public EnhancedMainWindow() {
        initializeServices();
        initializeComponents();
        setupStyles();
        setupEventHandlers();
        setupLayout();
        initializeExecutor();
        initializeDefaultSession();
    }

    /**
     * 初始化服务
     */
    private void initializeServices() {
        sessionManager = new SessionManager();
    }

    /**
     * 初始化GUI组件
     */
    private void initializeComponents() {
        setTitle("Claude Code CLI GUI - v2.0.0 (增强版)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 创建对话显示区域
        conversationArea = new JTextPane();
        conversationArea.setEditable(false);
        conversationArea.setFont(ThemeConfig.createDefaultFont());
        conversationArea.setBackground(Color.WHITE);
        document = conversationArea.getStyledDocument();

        // 创建对话区滚动面板
        conversationScrollPane = new JScrollPane(conversationArea);
        conversationScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        conversationScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        conversationScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());

        // 创建会话列表面板
        sessionListPanel = new SessionListPanel(sessionManager);
        sessionListPanel.setSelectionListener(this);

        // 创建输入框
        inputField = new JTextField();
        ThemeConfig.styleTextField(inputField);
        inputField.setEnabled(false);

        // 创建发送按钮
        sendButton = new JButton("发送");
        ThemeConfig.styleButton(sendButton, ThemeConfig.ButtonType.PRIMARY);
        sendButton.setEnabled(false);

        // 创建连接按钮
        connectButton = new JButton("连接Claude CLI");
        ThemeConfig.styleButton(connectButton, ThemeConfig.ButtonType.SUCCESS);

        // 创建状态标签
        statusLabel = new JLabel("未连接");
        ThemeConfig.styleLabel(statusLabel, ThemeConfig.LabelType.ERROR);
    }

    /**
     * 设置文本样式
     */
    private void setupStyles() {
        // 使用ThemeConfig配置样式
        ThemeConfig.configureTextStyles(conversationArea);

        // 获取已配置的样式
        userStyle = conversationArea.getStyle("user");
        claudeStyle = conversationArea.getStyle("claude");
        errorStyle = conversationArea.getStyle("error");
        systemStyle = conversationArea.getStyle("system");
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 发送按钮点击事件
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // 输入框回车事件
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        // 连接按钮点击事件
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleConnection();
            }
        });

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 创建主分割面板
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(sessionListPanel);

        // 右侧面板 - 对话区域和输入区域
        JPanel rightPanel = createRightPanel();
        mainSplitPane.setRightComponent(rightPanel);

        // 设置分割面板属性
        mainSplitPane.setDividerLocation(300);
        mainSplitPane.setResizeWeight(0.25);

        add(mainSplitPane, BorderLayout.CENTER);

        // 设置窗口背景色
        getContentPane().setBackground(ThemeConfig.BACKGROUND_COLOR);
    }

    /**
     * 创建右侧面板（对话区域）
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(ThemeConfig.BACKGROUND_COLOR);

        // 中央面板 - 对话显示区域
        rightPanel.add(conversationScrollPane, BorderLayout.CENTER);

        // 底部面板 - 输入区域
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.setBackground(ThemeConfig.BACKGROUND_COLOR);

        // 输入面板
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(ThemeConfig.BACKGROUND_COLOR);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        // 状态面板
        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        statusPanel.setBackground(ThemeConfig.BACKGROUND_COLOR);
        statusPanel.add(connectButton, BorderLayout.WEST);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        return rightPanel;
    }

    /**
     * 初始化CLI执行器
     */
    private void initializeExecutor() {
        cliExecutor = new SessionAwareClaudeExecutor();
        cliExecutor.setMessageCallback(this);
        cliExecutor.setUseResume(true); // 启用resume功能

        // Windows 交互兼容性：Claude CLI 非 REPL，默认使用 SDK 模式
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cliExecutor.setUsePtyMode(false);
            appendMessage("系统", "Windows 环境：使用 SDK 非交互模式（支持 --resume）", systemStyle);
        }

        // 显示欢迎消息
        appendMessage("系统", "欢迎使用Claude Code CLI GUI 增强版！", systemStyle);
        appendMessage("系统", "✨ 新功能：会话管理 - 创建、切换和管理多个对话会话", systemStyle);
        appendMessage("系统", "🔄 支持 --resume 功能，保持会话连续性", systemStyle);
        appendMessage("系统", "点击\"连接Claude CLI\"按钮开始使用。", systemStyle);
    }

    /**
     * 初始化默认会话
     */
    private void initializeDefaultSession() {
        ChatSession defaultSession = sessionManager.createNewSession("默认会话");
        sessionManager.switchToSession(defaultSession.getId());
        sessionListPanel.refreshSessionList();

        // 在执行器中设置会话ID
        cliExecutor.setCurrentSessionId(defaultSession.getId());
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        // 添加消息到当前会话
        sessionManager.addMessageToCurrentSession("用户", message, MessageType.USER);

        // 显示用户消息
        appendMessage("用户", message, userStyle);

        // 发送到Claude CLI
        if (cliExecutor.isRunning()) {
            cliExecutor.sendCommand(message);
        } else {
            appendMessage("错误", "Claude CLI未连接", errorStyle);
        }

        // 清空输入框
        inputField.setText("");

        // 刷新会话列表以更新预览
        sessionListPanel.refreshSessionList();
    }

    /**
     * 切换连接状态
     */
    private void toggleConnection() {
        if (cliExecutor.isRunning()) {
            // 断开连接
            disconnectClaude();
        } else {
            // 建立连接
            connectClaude();
        }
    }

    /**
     * 连接Claude CLI
     */
    private void connectClaude() {
        connectButton.setEnabled(false);
        connectButton.setText("连接中...");

        // 显示进度对话框
        ErrorHandler.ProgressDialog progressDialog = ErrorHandler.showProgress(
            this,
            "连接Claude CLI",
            "正在启动Claude CLI，请稍候..."
        );

        // 在后台线程中启动Claude CLI
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                cliExecutor.startClaudeProcess();
                return null;
            }

            @Override
            protected void done() {
                progressDialog.close();
                try {
                    get(); // 检查是否有异常
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String userMessage = ErrorHandler.createUserFriendlyMessage(cause);
                    appendMessage("错误", userMessage, errorStyle);

                    // 如果是连接相关错误，显示详细的解决建议
                    if (cause instanceof IOException) {
                        ErrorHandler.showConnectionError(EnhancedMainWindow.this);
                    } else {
                        ErrorHandler.showDetailedError(
                            EnhancedMainWindow.this,
                            "连接失败",
                            userMessage,
                            cause
                        );
                    }

                    connectButton.setText("连接Claude CLI");
                    connectButton.setEnabled(true);
                }
            }
        };
        worker.execute();

        // 显示进度对话框
        progressDialog.show();
    }

    /**
     * 断开Claude CLI连接
     */
    private void disconnectClaude() {
        cliExecutor.stopProcess();
    }

    /**
     * 退出应用程序
     */
    private void exitApplication() {
        boolean confirmed = ErrorHandler.showConfirm(
            this,
            "确认退出",
            "确定要退出Claude Code CLI GUI吗？"
        );

        if (confirmed) {
            if (cliExecutor.isRunning()) {
                cliExecutor.stopProcess();
            }
            System.exit(0);
        }
    }

    /**
     * 添加消息到对话区域
     */
    private void appendMessage(String sender, String message, Style style) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timeStr = TIME_FORMAT.format(new Date());
                String fullMessage = String.format("[%s] %s: %s\n", timeStr, sender, message);

                // 检查是否超过最大行数限制
                if (document.getLength() > MAX_HISTORY_LINES * 100) {
                    // 清除前面的部分内容
                    document.remove(0, document.getLength() / 2);
                }

                document.insertString(document.getLength(), fullMessage, style);

                // 自动滚动到底部
                conversationArea.setCaretPosition(document.getLength());

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 从会话历史恢复对话显示
     */
    private void restoreConversationFromSession(ChatSession session) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 清空当前对话显示
                document.remove(0, document.getLength());

                // 恢复会话消息
                List<com.claude.gui.model.Message> messages = session.getMessages();
                for (com.claude.gui.model.Message msg : messages) {
                    Style style = getStyleForMessageType(msg.getType());
                    String fullMessage = String.format("[%s] %s: %s\n",
                        msg.getFormattedTime(),
                        msg.getSender(),
                        msg.getContent());
                    document.insertString(document.getLength(), fullMessage, style);
                }

                // 自动滚动到底部
                conversationArea.setCaretPosition(document.getLength());

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 根据消息类型获取样式
     */
    private Style getStyleForMessageType(MessageType messageType) {
        switch (messageType) {
            case USER:
                return userStyle;
            case CLAUDE:
                return claudeStyle;
            case ERROR:
                return errorStyle;
            case SYSTEM:
            default:
                return systemStyle;
        }
    }

    // MessageCallback接口实现

    @Override
    public void onMessageReceived(String message) {
        // 添加消息到当前会话
        sessionManager.addMessageToCurrentSession("Claude", message, MessageType.CLAUDE);

        appendMessage("Claude", message, claudeStyle);

        // 刷新会话列表以更新预览
        sessionListPanel.refreshSessionList();
    }

    @Override
    public void onError(String error) {
        // 添加错误消息到当前会话
        sessionManager.addMessageToCurrentSession("错误", error, MessageType.ERROR);

        appendMessage("错误", error, errorStyle);

        // 刷新会话列表
        sessionListPanel.refreshSessionList();
    }

    @Override
    public void onProcessFinished() {
        SwingUtilities.invokeLater(() -> {
            appendMessage("系统", "Claude CLI进程已结束", systemStyle);
            connectButton.setText("连接Claude CLI");
            connectButton.setEnabled(true);
            sendButton.setEnabled(false);
            inputField.setEnabled(false);
            statusLabel.setText("未连接");
            statusLabel.setForeground(Color.RED);
        });
    }

    @Override
    public void onProcessStarted() {
        SwingUtilities.invokeLater(() -> {
            appendMessage("系统", "Claude CLI已启动，可以开始对话", systemStyle);
            connectButton.setText("断开连接");
            connectButton.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.setEnabled(true);
            inputField.requestFocus();
        });
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                statusLabel.setText("已连接");
                ThemeConfig.styleLabel(statusLabel, ThemeConfig.LabelType.SUCCESS);
            } else {
                statusLabel.setText("未连接");
                ThemeConfig.styleLabel(statusLabel, ThemeConfig.LabelType.ERROR);
            }
        });
    }

    // SessionSelectionListener接口实现

    @Override
    public void onSessionSelected(ChatSession session) {
        // 切换会话
        boolean switched = sessionManager.switchToSession(session.getId());
        if (switched) {
            // 在执行器中设置新的会话ID
            if (cliExecutor.isRunning()) {
                cliExecutor.resumeSession(session.getId());
            } else {
                cliExecutor.setCurrentSessionId(session.getId());
            }

            // 恢复会话的对话显示
            restoreConversationFromSession(session);

            // 添加会话切换提示
            appendMessage("系统", "已切换到会话: " + session.getName(), systemStyle);
        }
    }

    @Override
    public void onSessionDeleted(ChatSession session) {
        // 如果删除的是当前会话，切换到其他会话或创建新会话
        if (!sessionManager.hasActiveSession()) {
            // 创建新的默认会话
            ChatSession newSession = sessionManager.createNewSession("新会话");
            sessionManager.switchToSession(newSession.getId());
            cliExecutor.setCurrentSessionId(newSession.getId());

            // 清空对话显示
            try {
                document.remove(0, document.getLength());
                appendMessage("系统", "已创建新会话: " + newSession.getName(), systemStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            sessionListPanel.refreshSessionList();
        }
    }

    @Override
    public void onNewSessionCreated(ChatSession session) {
        // 切换到新创建的会话
        boolean switched = sessionManager.switchToSession(session.getId());
        if (switched) {
            cliExecutor.setCurrentSessionId(session.getId());

            // 清空对话显示并显示欢迎消息
            try {
                document.remove(0, document.getLength());
                appendMessage("系统", "已创建新会话: " + session.getName(), systemStyle);
                appendMessage("系统", "开始新的对话吧！", systemStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
}
