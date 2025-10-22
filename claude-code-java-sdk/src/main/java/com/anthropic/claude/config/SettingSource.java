package com.anthropic.claude.config;

/**
 * 配置源枚举
 *
 * <p>定义可用的配置来源及其优先级。数字越大，优先级越高。</p>
 *
 * <h3>优先级顺序</h3>
 * <ol>
 *   <li>DEFAULTS (0) - 默认值</li>
 *   <li>ENV_VARS (1) - 环境变量</li>
 *   <li>USER_CONFIG (2) - 用户配置文件</li>
 *   <li>PROJECT_CONFIG (3) - 项目配置文件</li>
 *   <li>CLAUDE_MD (4) - CLAUDE.md 文件</li>
 *   <li>SLASH_COMMANDS (5) - Slash commands</li>
 *   <li>INLINE_OPTIONS (10) - 内联选项（最高优先级）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 自定义配置源
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .settingSources(
 *         SettingSource.ENV_VARS,
 *         SettingSource.USER_CONFIG,
 *         SettingSource.INLINE_OPTIONS
 *     )
 *     .build();
 *
 * // v1.x 兼容模式（加载所有源）
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .useV1CompatibilityMode()
 *     .build();
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public enum SettingSource {
    /**
     * 默认值（优先级最低）
     */
    DEFAULTS("defaults", 0),

    /**
     * 环境变量
     * <p>例如：ANTHROPIC_API_KEY, CLAUDE_CODE_CLI_PATH</p>
     */
    ENV_VARS("env", 1),

    /**
     * 用户配置文件 (~/.claude/config.properties)
     */
    USER_CONFIG("user_config", 2),

    /**
     * 项目配置文件 (./.claude/config.properties)
     */
    PROJECT_CONFIG("project_config", 3),

    /**
     * CLAUDE.md 文件
     * <p>包含项目特定的 Claude 指令</p>
     */
    CLAUDE_MD("claude_md", 4),

    /**
     * Slash commands 目录 (./.claude/commands/)
     */
    SLASH_COMMANDS("slash_commands", 5),

    /**
     * 内联选项（优先级最高）
     * <p>通过 Builder API 直接设置的选项</p>
     */
    INLINE_OPTIONS("inline", 10);

    private final String name;
    private final int priority;

    /**
     * 构造函数
     *
     * @param name 源名称
     * @param priority 优先级（数字越大优先级越高）
     */
    SettingSource(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    /**
     * 获取源名称
     *
     * @return 源名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取优先级
     *
     * @return 优先级（数字越大优先级越高）
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 根据名称查找配置源
     *
     * @param name 源名称
     * @return 配置源，如果未找到返回 null
     */
    public static SettingSource fromName(String name) {
        if (name == null) {
            return null;
        }

        for (SettingSource source : values()) {
            if (source.name.equalsIgnoreCase(name)) {
                return source;
            }
        }

        return null;
    }

    /**
     * 检查是否为文件系统配置源
     *
     * @return 如果是文件系统配置源返回 true
     */
    public boolean isFilesystemSource() {
        return this == USER_CONFIG ||
               this == PROJECT_CONFIG ||
               this == CLAUDE_MD ||
               this == SLASH_COMMANDS;
    }

    @Override
    public String toString() {
        return String.format("%s(priority=%d)", name, priority);
    }
}
