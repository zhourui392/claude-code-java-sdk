package com.claude.gui.ui;

import com.claude.gui.callback.MessageCallback;
import com.claude.gui.executor.ClaudeCliExecutor;

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

/**
 * 主窗口类，提供Swing GUI界面
 * 实现类似聊天对话框的用户界面
 *
 * @author Claude Code GUI Team
 * @version 1.0.0
 */
public class MainWindow extends JFrame implements MessageCallback {

    private static final int MAX_HISTORY_LINES = 1000;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // GUI组件
    private JTextPane conversationArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JLabel statusLabel;
    private JScrollPane scrollPane;

    // 业务组件
    private ClaudeCliExecutor cliExecutor;

    // 样式
    private StyledDocument document;
    private Style userStyle;
    private Style claudeStyle;
    private Style errorStyle;
    private Style systemStyle;

    /**
     * 构造函数
     */
    public MainWindow() {
        initializeComponents();
        setupStyles();
        setupEventHandlers();
        setupLayout();
        initializeExecutor();
    }

    /**
     * 初始化GUI组件
     */
    private void initializeComponents() {
        setTitle("Claude Code CLI GUI - v1.0.0");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // 创建对话显示区域
        conversationArea = new JTextPane();
        conversationArea.setEditable(false);
        conversationArea.setFont(ThemeConfig.createDefaultFont());
        conversationArea.setBackground(Color.WHITE);
        document = conversationArea.getStyledDocument();

        // 创建滚动面板
        scrollPane = new JScrollPane(conversationArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());

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

        // 中央面板 - 对话显示区域
        add(scrollPane, BorderLayout.CENTER);

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
        add(bottomPanel, BorderLayout.SOUTH);

        // 设置窗口背景色
        getContentPane().setBackground(ThemeConfig.BACKGROUND_COLOR);
    }

    /**
     * 初始化CLI执行器
     */
    private void initializeExecutor() {
        cliExecutor = new ClaudeCliExecutor();
        cliExecutor.setMessageCallback(this);

        // 显示欢迎消息
        appendMessage("系统", "欢迎使用Claude Code CLI GUI！", systemStyle);
        appendMessage("系统", "点击\"连接Claude CLI\"按钮开始使用。", systemStyle);
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

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
                        ErrorHandler.showConnectionError(MainWindow.this);
                    } else {
                        ErrorHandler.showDetailedError(
                            MainWindow.this,
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
     *
     * @param sender  发送者
     * @param message 消息内容
     * @param style   文本样式
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

    // MessageCallback接口实现

    @Override
    public void onMessageReceived(String message) {
        appendMessage("Claude", message, claudeStyle);
    }

    @Override
    public void onError(String error) {
        appendMessage("错误", error, errorStyle);
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
}