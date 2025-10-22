package com.anthropic.claude.subagents;

import java.time.Duration;
import java.util.*;

/**
 * 子代理定义
 *
 * <p>用于在代码中定义子代理，而非依赖文件系统配置。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 定义一个代码审查子代理
 * SubagentDefinition codeReviewer = SubagentDefinition.builder("code-reviewer")
 *     .description("Reviews code quality and suggests improvements")
 *     .systemPrompt("You are an expert code reviewer specializing in Java")
 *     .tools("Read", "Grep", "Glob")
 *     .model("claude-sonnet-4-5")
 *     .build();
 *
 * // 在配置中使用
 * ClaudeAgentOptions options = ClaudeAgentOptions.builder()
 *     .addAgent(codeReviewer)
 *     .build();
 *
 * // 或运行时注册
 * sdk.getSubagentManager().registerInlineAgent(codeReviewer);
 * }</pre>
 *
 * @author Claude Agent SDK Team
 * @version 2.0.0
 * @since 2.0.0
 */
public class SubagentDefinition {

    private final String name;
    private final String description;
    private final String systemPrompt;
    private final List<String> tools;
    private final String model;
    private final Map<String, Object> metadata;
    private final Duration timeout;
    private final int maxRetries;

    private SubagentDefinition(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.description = Objects.requireNonNull(builder.description, "description cannot be null");
        this.systemPrompt = Objects.requireNonNull(builder.systemPrompt, "systemPrompt cannot be null");
        this.tools = Collections.unmodifiableList(new ArrayList<>(builder.tools));
        this.model = builder.model;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
    }

    /**
     * 获取子代理名称
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取描述（何时使用此子代理）
     *
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取系统提示词
     *
     * @return 系统提示词
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 获取可用工具列表
     *
     * @return 不可变的工具列表
     */
    public List<String> getTools() {
        return tools;
    }

    /**
     * 获取使用的模型
     *
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 获取元数据
     *
     * @return 不可变的元数据 Map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 获取超时时间
     *
     * @return 超时时间
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 转换为 CLI 参数
     *
     * <p>将子代理定义转换为 Claude CLI 可接受的参数列表</p>
     *
     * @return CLI 参数列表
     */
    public List<String> toCliArgs() {
        List<String> args = new ArrayList<>();

        args.add("--agent-name");
        args.add(name);

        args.add("--agent-description");
        args.add(description);

        args.add("--agent-prompt");
        args.add(systemPrompt);

        if (!tools.isEmpty()) {
            args.add("--agent-tools");
            args.add(String.join(",", tools));
        }

        args.add("--model");
        args.add(model);

        return args;
    }

    /**
     * 创建 Builder 实例
     *
     * @param name 子代理名称
     * @return Builder 实例
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * SubagentDefinition Builder
     */
    public static class Builder {
        private final String name;
        private String description;
        private String systemPrompt;
        private List<String> tools = new ArrayList<>();
        private String model = "claude-sonnet-4-5";
        private Map<String, Object> metadata = new HashMap<>();
        private Duration timeout = Duration.ofMinutes(10);
        private int maxRetries = 3;

        /**
         * 创建 Builder
         *
         * @param name 子代理名称（必需）
         */
        public Builder(String name) {
            this.name = name;
        }

        /**
         * 设置描述
         *
         * @param description 描述（何时使用此子代理）
         * @return this
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * 设置系统提示词
         *
         * @param systemPrompt 系统提示词
         * @return this
         */
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * 添加可用工具（多个）
         *
         * @param tools 工具名称
         * @return this
         */
        public Builder tools(String... tools) {
            this.tools.addAll(Arrays.asList(tools));
            return this;
        }

        /**
         * 设置可用工具列表
         *
         * @param tools 工具列表
         * @return this
         */
        public Builder tools(List<String> tools) {
            this.tools.addAll(tools);
            return this;
        }

        /**
         * 设置使用的模型
         *
         * @param model 模型名称（默认: claude-sonnet-4-5）
         * @return this
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 设置元数据
         *
         * @param metadata 元数据 Map
         * @return this
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * 添加元数据条目
         *
         * @param key 键
         * @param value 值
         * @return this
         */
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * 设置超时时间
         *
         * @param timeout 超时时间（默认: 10 分钟）
         * @return this
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置最大重试次数
         *
         * @param maxRetries 最大重试次数（默认: 3）
         * @return this
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * 构建 SubagentDefinition 实例
         *
         * @return SubagentDefinition 实例
         * @throws IllegalStateException 如果必需字段未设置
         */
        public SubagentDefinition build() {
            if (description == null || systemPrompt == null) {
                throw new IllegalStateException(
                    "description and systemPrompt are required fields"
                );
            }
            return new SubagentDefinition(this);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "SubagentDefinition{name='%s', model='%s', tools=%d}",
            name, model, tools.size()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubagentDefinition that = (SubagentDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
