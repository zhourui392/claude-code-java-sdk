package com.claude.gui.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 错误处理和用户提示工具类
 * 提供统一的错误显示和用户交互对话框
 *
 * @author Claude Code GUI Team
 * @version 1.0.0
 */
public class ErrorHandler {

    /**
     * 显示错误消息对话框
     *
     * @param parent  父窗口
     * @param title   对话框标题
     * @param message 错误消息
     */
    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * 显示警告消息对话框
     *
     * @param parent  父窗口
     * @param title   对话框标题
     * @param message 警告消息
     */
    public static void showWarning(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title,
            JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * 显示信息消息对话框
     *
     * @param parent  父窗口
     * @param title   对话框标题
     * @param message 信息消息
     */
    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            title,
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * 显示确认对话框
     *
     * @param parent  父窗口
     * @param title   对话框标题
     * @param message 确认消息
     * @return 用户选择（true表示确认，false表示取消）
     */
    public static boolean showConfirm(Component parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(
            parent,
            message,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * 显示详细的异常信息对话框
     *
     * @param parent    父窗口
     * @param title     对话框标题
     * @param message   主要错误消息
     * @param exception 异常对象
     */
    public static void showDetailedError(Component parent, String title, String message, Throwable exception) {
        // 创建主要消息面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(message), BorderLayout.NORTH);

        // 创建详细信息按钮
        JButton detailsButton = new JButton("显示详细信息");
        JTextArea detailsArea = new JTextArea(10, 50);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // 获取异常堆栈跟踪
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        detailsArea.setText(sw.toString());

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setVisible(false);

        // 详细信息按钮点击事件
        detailsButton.addActionListener(new ActionListener() {
            private boolean detailsVisible = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                detailsVisible = !detailsVisible;
                scrollPane.setVisible(detailsVisible);
                detailsButton.setText(detailsVisible ? "隐藏详细信息" : "显示详细信息");

                // 重新调整对话框大小
                Window window = SwingUtilities.getWindowAncestor(panel);
                if (window != null) {
                    window.pack();
                }
            }
        });

        panel.add(detailsButton, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(
            parent,
            panel,
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * 显示连接失败的错误对话框，提供解决建议
     *
     * @param parent 父窗口
     */
    public static void showConnectionError(Component parent) {
        String message = "连接Claude CLI失败！\n\n" +
                "可能的原因和解决方案：\n" +
                "1. Claude CLI未安装或未在PATH中\n" +
                "   解决方案：请安装Claude CLI或确保其在系统PATH中\n\n" +
                "2. Claude CLI版本不兼容\n" +
                "   解决方案：请更新到最新版本的Claude CLI\n\n" +
                "3. 网络连接问题\n" +
                "   解决方案：请检查网络连接状态\n\n" +
                "4. 权限问题\n" +
                "   解决方案：请以管理员权限运行程序";

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        textArea.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(
            parent,
            scrollPane,
            "连接失败",
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * 显示命令执行失败的错误对话框
     *
     * @param parent  父窗口
     * @param command 失败的命令
     * @param error   错误信息
     */
    public static void showCommandError(Component parent, String command, String error) {
        String message = String.format(
            "命令执行失败！\n\n" +
            "命令：%s\n" +
            "错误：%s\n\n" +
            "建议：\n" +
            "• 检查命令语法是否正确\n" +
            "• 确保Claude CLI正常运行\n" +
            "• 重新连接后再试",
            command, error
        );

        showError(parent, "命令执行失败", message);
    }

    /**
     * 显示进度对话框
     *
     * @param parent  父窗口
     * @param title   对话框标题
     * @param message 进度消息
     * @return 进度对话框实例
     */
    public static ProgressDialog showProgress(Component parent, String title, String message) {
        return new ProgressDialog(parent, title, message);
    }

    /**
     * 进度对话框类
     */
    public static class ProgressDialog {
        private JDialog dialog;
        private JProgressBar progressBar;
        private JLabel messageLabel;
        private volatile boolean cancelled = false;

        public ProgressDialog(Component parent, String title, String message) {
            dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            messageLabel = new JLabel(message);
            panel.add(messageLabel, BorderLayout.NORTH);

            progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            panel.add(progressBar, BorderLayout.CENTER);

            JButton cancelButton = new JButton("取消");
            cancelButton.addActionListener(e -> {
                cancelled = true;
                dialog.dispose();
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(cancelButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
        }

        public void setMessage(String message) {
            SwingUtilities.invokeLater(() -> messageLabel.setText(message));
        }

        public void setProgress(int value) {
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setValue(value);
            });
        }

        public void show() {
            SwingUtilities.invokeLater(() -> dialog.setVisible(true));
        }

        public void close() {
            SwingUtilities.invokeLater(() -> dialog.dispose());
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * 创建用户友好的错误消息
     *
     * @param exception 异常对象
     * @return 用户友好的错误消息
     */
    public static String createUserFriendlyMessage(Throwable exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();

        switch (className) {
            case "IOException":
                return "文件或网络操作失败：" + (message != null ? message : "未知错误");
            case "IllegalArgumentException":
                return "参数错误：" + (message != null ? message : "提供的参数不正确");
            case "IllegalStateException":
                return "状态错误：" + (message != null ? message : "当前操作不被允许");
            case "SecurityException":
                return "权限错误：" + (message != null ? message : "没有足够的权限执行此操作");
            case "InterruptedException":
                return "操作被中断：" + (message != null ? message : "操作被用户或系统中断");
            default:
                return "发生错误：" + (message != null ? message : "未知错误类型：" + className);
        }
    }
}