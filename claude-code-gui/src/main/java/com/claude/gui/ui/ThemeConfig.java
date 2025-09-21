package com.claude.gui.ui;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

/**
 * 界面主题和样式配置类
 * 提供统一的界面样式和主题管理
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class ThemeConfig {

    // 颜色配置
    public static final Color PRIMARY_COLOR = new Color(0, 102, 204);
    public static final Color SUCCESS_COLOR = new Color(0, 153, 76);
    public static final Color ERROR_COLOR = new Color(220, 53, 69);
    public static final Color WARNING_COLOR = new Color(255, 193, 7);
    public static final Color SECONDARY_COLOR = new Color(108, 117, 125);
    public static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    public static final Color BORDER_COLOR = new Color(206, 212, 218);
    public static final Color HOVER_COLOR = new Color(230, 240, 255);
    public static final Color TEXT_COLOR = new Color(33, 37, 41);

    // 字体配置
    public static final String DEFAULT_FONT_NAME = getDefaultFontName();
    public static final int DEFAULT_FONT_SIZE = 14;
    public static final int SMALL_FONT_SIZE = 12;
    public static final int LARGE_FONT_SIZE = 16;

    /**
     * 获取系统默认字体名称
     */
    private static String getDefaultFontName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return "Microsoft YaHei";
        } else if (os.contains("mac")) {
            return "PingFang SC";
        } else {
            return "Noto Sans CJK SC";
        }
    }

    /**
     * 创建默认字体
     */
    public static Font createDefaultFont() {
        return new Font(DEFAULT_FONT_NAME, Font.PLAIN, DEFAULT_FONT_SIZE);
    }

    /**
     * 创建指定大小的默认字体
     */
    public static Font createDefaultFont(int size) {
        return new Font(DEFAULT_FONT_NAME, Font.PLAIN, size);
    }

    /**
     * 创建粗体字体
     */
    public static Font createBoldFont() {
        return new Font(DEFAULT_FONT_NAME, Font.BOLD, DEFAULT_FONT_SIZE);
    }

    /**
     * 创建指定大小的粗体字体
     */
    public static Font createBoldFont(int size) {
        return new Font(DEFAULT_FONT_NAME, Font.BOLD, size);
    }

    /**
     * 创建小号字体
     */
    public static Font createSmallFont() {
        return new Font(DEFAULT_FONT_NAME, Font.PLAIN, SMALL_FONT_SIZE);
    }

    /**
     * 创建大号字体
     */
    public static Font createLargeFont() {
        return new Font(DEFAULT_FONT_NAME, Font.PLAIN, LARGE_FONT_SIZE);
    }

    /**
     * 创建等宽字体
     */
    public static Font createMonospaceFont() {
        return new Font(Font.MONOSPACED, Font.PLAIN, SMALL_FONT_SIZE);
    }

    /**
     * 配置文本样式
     */
    public static void configureTextStyles(JTextPane textPane) {
        StyledDocument document = textPane.getStyledDocument();

        // 用户消息样式
        Style userStyle = textPane.addStyle("user", null);
        StyleConstants.setForeground(userStyle, PRIMARY_COLOR);
        StyleConstants.setBold(userStyle, true);
        StyleConstants.setFontFamily(userStyle, DEFAULT_FONT_NAME);
        StyleConstants.setFontSize(userStyle, DEFAULT_FONT_SIZE);

        // Claude响应样式
        Style claudeStyle = textPane.addStyle("claude", null);
        StyleConstants.setForeground(claudeStyle, SUCCESS_COLOR);
        StyleConstants.setFontFamily(claudeStyle, DEFAULT_FONT_NAME);
        StyleConstants.setFontSize(claudeStyle, DEFAULT_FONT_SIZE);

        // 错误消息样式
        Style errorStyle = textPane.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, ERROR_COLOR);
        StyleConstants.setBold(errorStyle, true);
        StyleConstants.setFontFamily(errorStyle, DEFAULT_FONT_NAME);
        StyleConstants.setFontSize(errorStyle, DEFAULT_FONT_SIZE);

        // 系统消息样式
        Style systemStyle = textPane.addStyle("system", null);
        StyleConstants.setForeground(systemStyle, SECONDARY_COLOR);
        StyleConstants.setItalic(systemStyle, true);
        StyleConstants.setFontFamily(systemStyle, DEFAULT_FONT_NAME);
        StyleConstants.setFontSize(systemStyle, SMALL_FONT_SIZE);

        // 时间戳样式
        Style timestampStyle = textPane.addStyle("timestamp", null);
        StyleConstants.setForeground(timestampStyle, SECONDARY_COLOR);
        StyleConstants.setFontFamily(timestampStyle, Font.MONOSPACED);
        StyleConstants.setFontSize(timestampStyle, SMALL_FONT_SIZE);
    }

    /**
     * 应用按钮样式
     */
    public static void styleButton(JButton button, ButtonType type) {
        button.setFont(createDefaultFont());
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);

        switch (type) {
            case PRIMARY:
                button.setBackground(PRIMARY_COLOR);
                button.setForeground(Color.WHITE);
                break;
            case SUCCESS:
                button.setBackground(SUCCESS_COLOR);
                button.setForeground(Color.WHITE);
                break;
            case DANGER:
                button.setBackground(ERROR_COLOR);
                button.setForeground(Color.WHITE);
                break;
            case WARNING:
                button.setBackground(WARNING_COLOR);
                button.setForeground(Color.BLACK);
                break;
            case SECONDARY:
                button.setBackground(Color.WHITE);
                button.setForeground(TEXT_COLOR);
                button.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
                break;
            default:
                button.setBackground(Color.WHITE);
                button.setForeground(TEXT_COLOR);
                break;
        }
    }

    /**
     * 应用文本框样式
     */
    public static void styleTextField(JTextField textField) {
        textField.setFont(createDefaultFont());
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
    }

    /**
     * 应用标签样式
     */
    public static void styleLabel(JLabel label, LabelType type) {
        label.setFont(createDefaultFont());

        switch (type) {
            case SUCCESS:
                label.setForeground(SUCCESS_COLOR);
                break;
            case ERROR:
                label.setForeground(ERROR_COLOR);
                break;
            case WARNING:
                label.setForeground(WARNING_COLOR);
                break;
            case SECONDARY:
                label.setForeground(SECONDARY_COLOR);
                break;
            default:
                label.setForeground(TEXT_COLOR);
                break;
        }
    }

    /**
     * 设置全局主题
     */
    public static void setupGlobalLookAndFeel() {
        try {
            // 使用系统外观
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());

            // 设置全局字体
            Font defaultFont = createDefaultFont();
            UIManager.put("Button.font", defaultFont);
            UIManager.put("Label.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("TextArea.font", defaultFont);
            UIManager.put("List.font", defaultFont);
            UIManager.put("ComboBox.font", defaultFont);

        } catch (Exception e) {
            System.err.println("无法设置外观主题: " + e.getMessage());
        }
    }

    /**
     * 按钮类型枚举
     */
    public enum ButtonType {
        PRIMARY, SUCCESS, DANGER, WARNING, SECONDARY
    }

    /**
     * 标签类型枚举
     */
    public enum LabelType {
        DEFAULT, SUCCESS, ERROR, WARNING, SECONDARY
    }
}