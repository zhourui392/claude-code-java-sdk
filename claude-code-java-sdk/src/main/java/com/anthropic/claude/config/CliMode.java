package com.anthropic.claude.config;

/**
 * CLI执行模式枚举
 *
 * @author Claude Code SDK
 */
public enum CliMode {
    /**
     * 批处理模式（默认）
     * 每次调用启动CLI，读取输出后进程结束
     * 默认采用 --print 参数
     */
    BATCH("批处理模式"),

    /**
     * PTY交互模式（可选）
     * 维护常驻会话，向stdin写入，按行监听stdout
     * 异常时自动回退至批处理模式
     */
    PTY_INTERACTIVE("PTY交互模式");

    private final String description;

    CliMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取默认CLI模式
     * Windows 11环境下默认使用批处理模式
     */
    public static CliMode getDefault() {
        return BATCH;
    }
}