package com.claude.gui;

import com.claude.gui.ui.EnhancedMainWindow;
import com.claude.gui.ui.ThemeConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Claude Code CLI GUI应用程序主入口类
 * 增强版 - 支持会话管理和--resume功能
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class ClaudeCodeGUI {

    /**
     * 应用程序主入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置系统属性，确保正确的编码和渲染
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        System.setProperty("java.awt.headless", "false");

        // 强制设置控制台输出编码为UTF-8（Windows兼容性）
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            // 如果UTF-8不可用，fallback到系统默认编码
            System.err.println("Warning: UTF-8 encoding not available, using system default");
        }

        // 设置全局主题和样式
        ThemeConfig.setupGlobalLookAndFeel();

        // 设置字体渲染提示，提高中文显示效果
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // 在EDT线程中创建和显示GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    createAndShowGUI();
                } catch (Exception e) {
                    handleFatalError("启动应用程序失败", e);
                }
            }
        });
    }

    /**
     * 创建并显示GUI界面
     */
    private static void createAndShowGUI() {
        try {
            // 创建增强主窗口
            EnhancedMainWindow mainWindow = new EnhancedMainWindow();

            // 设置窗口图标（如果有的话）
            setApplicationIcon(mainWindow);

            // 显示窗口
            mainWindow.setVisible(true);

            System.out.println("Claude Code CLI GUI 增强版已启动");
            System.out.println("✨ 新功能：会话管理、--resume支持");

        } catch (Exception e) {
            handleFatalError("创建主窗口失败", e);
        }
    }

    /**
     * 设置应用程序图标
     *
     * @param window 主窗口
     */
    private static void setApplicationIcon(Window window) {
        try {
            // 尝试加载应用程序图标
            // 这里可以放置应用程序图标文件
            // ImageIcon icon = new ImageIcon(ClaudeCodeGUI.class.getResource("/icon.png"));
            // window.setIconImage(icon.getImage());
        } catch (Exception e) {
            // 图标加载失败，使用默认图标
            System.out.println("加载应用程序图标失败，使用默认图标");
        }
    }


    /**
     * 处理致命错误
     *
     * @param message 错误消息
     * @param e       异常对象
     */
    private static void handleFatalError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();

        // 显示错误对话框
        try {
            String errorMessage = message + "\n\n" +
                    "错误详情: " + e.getMessage() + "\n\n" +
                    "请检查以下事项：\n" +
                    "1. 确保已安装JDK 1.8或更高版本\n" +
                    "2. 确保Claude Code CLI已正确安装\n" +
                    "3. 检查系统环境变量配置\n\n" +
                    "如果问题持续存在，请联系技术支持。";

            JOptionPane.showMessageDialog(
                    null,
                    errorMessage,
                    "Claude Code CLI GUI - 启动错误",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception dialogException) {
            // 连对话框都无法显示，输出到控制台
            System.err.println("无法显示错误对话框: " + dialogException.getMessage());
        }

        // 退出应用程序
        System.exit(1);
    }

    /**
     * 获取应用程序版本信息
     *
     * @return 版本字符串
     */
    public static String getVersion() {
        return "2.0.0";
    }

    /**
     * 获取应用程序名称
     *
     * @return 应用程序名称
     */
    public static String getApplicationName() {
        return "Claude Code CLI GUI Enhanced";
    }

    /**
     * 打印版本信息
     */
    private static void printVersionInfo() {
        System.out.println(getApplicationName() + " v" + getVersion());
        System.out.println("Java版本: " + System.getProperty("java.version"));
        System.out.println("操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("系统架构: " + System.getProperty("os.arch"));
    }
}