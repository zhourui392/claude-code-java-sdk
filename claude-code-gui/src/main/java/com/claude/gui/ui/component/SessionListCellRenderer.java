package com.claude.gui.ui.component;

import com.claude.gui.model.ChatSession;
import com.claude.gui.ui.ThemeConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 会话项渲染器，用于自定义会话列表的显示
 *
 * @author Claude Code GUI Team
 * @version 2.0.0
 */
public class SessionListCellRenderer extends JPanel implements ListCellRenderer<ChatSession> {

    private JLabel nameLabel;
    private JLabel previewLabel;
    private JLabel timeLabel;
    private JLabel messageCountLabel;
    private boolean isSelected;
    private boolean isActive;

    public SessionListCellRenderer() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        setOpaque(true);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        // 会话名称标签
        nameLabel = new JLabel();
        nameLabel.setFont(ThemeConfig.createBoldFont(14));

        // 预览文本标签
        previewLabel = new JLabel();
        previewLabel.setFont(ThemeConfig.createDefaultFont(12));
        previewLabel.setForeground(Color.GRAY);

        // 时间标签
        timeLabel = new JLabel();
        timeLabel.setFont(ThemeConfig.createDefaultFont(10));
        timeLabel.setForeground(Color.LIGHT_GRAY);

        // 消息数量标签
        messageCountLabel = new JLabel();
        messageCountLabel.setFont(ThemeConfig.createDefaultFont(10));
        messageCountLabel.setForeground(Color.GRAY);
        messageCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(8, 4));

        // 左侧主要内容
        JPanel mainPanel = new JPanel(new BorderLayout(0, 2));
        mainPanel.setOpaque(false);

        // 顶部：会话名称
        mainPanel.add(nameLabel, BorderLayout.NORTH);

        // 中间：预览文本
        mainPanel.add(previewLabel, BorderLayout.CENTER);

        // 底部：时间信息
        mainPanel.add(timeLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // 右侧：消息计数
        add(messageCountLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ChatSession> list,
            ChatSession session,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        this.isSelected = isSelected;
        this.isActive = session.isActive();

        // 设置文本内容
        nameLabel.setText(session.getName());
        previewLabel.setText(session.getPreview());
        timeLabel.setText(session.getFormattedLastActiveTime());
        messageCountLabel.setText(String.valueOf(session.getMessageCount()));

        // 设置颜色和样式
        updateColors();

        return this;
    }

    private void updateColors() {
        Color backgroundColor;
        Color textColor;
        Color previewColor;

        if (isActive) {
            // 当前活跃会话
            backgroundColor = ThemeConfig.PRIMARY_COLOR;
            textColor = Color.WHITE;
            previewColor = new Color(255, 255, 255, 180);
            nameLabel.setForeground(textColor);
            previewLabel.setForeground(previewColor);
            timeLabel.setForeground(previewColor);
            messageCountLabel.setForeground(previewColor);
        } else if (isSelected) {
            // 选中但非活跃会话
            backgroundColor = ThemeConfig.HOVER_COLOR;
            textColor = ThemeConfig.TEXT_COLOR;
            previewColor = Color.GRAY;
            nameLabel.setForeground(textColor);
            previewLabel.setForeground(previewColor);
            timeLabel.setForeground(Color.LIGHT_GRAY);
            messageCountLabel.setForeground(Color.GRAY);
        } else {
            // 普通会话
            backgroundColor = Color.WHITE;
            textColor = ThemeConfig.TEXT_COLOR;
            previewColor = Color.GRAY;
            nameLabel.setForeground(textColor);
            previewLabel.setForeground(previewColor);
            timeLabel.setForeground(Color.LIGHT_GRAY);
            messageCountLabel.setForeground(Color.GRAY);
        }

        setBackground(backgroundColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 绘制底部分割线
        if (!isActive && !isSelected) {
            g.setColor(new Color(0, 0, 0, 20));
            g.drawLine(12, getHeight() - 1, getWidth() - 12, getHeight() - 1);
        }
    }
}