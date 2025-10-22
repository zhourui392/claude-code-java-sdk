package com.anthropic.claude.subagents;

import com.anthropic.claude.config.ClaudeAgentOptions;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 子代理管理器
 *
 * <p>负责子代理的启动、停止、查询和生命周期管理。</p>
 *
 * @author Claude Code Java SDK
 * @version 2.0.0
 */
public class SubagentManager {
    private static final Logger logger = LoggerFactory.getLogger(SubagentManager.class);

    private final ProcessManager processManager;
    private final Object options; // Can be ClaudeCodeOptions or ClaudeAgentOptions
    private final Map<String, Subagent> activeSubagents;
    private final Map<String, SubagentDefinition> inlineAgents;
    private final AtomicInteger subagentIdCounter = new AtomicInteger(0);

    /**
     * 创建子代理管理器（v1.0.0 兼容）
     *
     * @param processManager 进程管理器
     * @param options v1.0.0 配置
     * @deprecated 使用 {@link #SubagentManager(ProcessManager, ClaudeAgentOptions)}
     */
    @Deprecated
    public SubagentManager(ProcessManager processManager, ClaudeCodeOptions options) {
        this.processManager = processManager;
        this.options = options;
        this.activeSubagents = new ConcurrentHashMap<>();
        this.inlineAgents = new ConcurrentHashMap<>();
        logger.debug("子代理管理器已初始化（v1.0.0 兼容模式）");
    }

    /**
     * 创建子代理管理器（v2.0.0）
     *
     * @param processManager 进程管理器
     * @param options v2.0.0 配置
     */
    public SubagentManager(ProcessManager processManager, ClaudeAgentOptions options) {
        this.processManager = processManager;
        this.options = options;
        this.activeSubagents = new ConcurrentHashMap<>();
        this.inlineAgents = new ConcurrentHashMap<>();
        logger.debug("子代理管理器已初始化");
    }

    public String startSubagent(String type, Map<String, Object> config) throws ClaudeCodeException {
        String subagentId = "subagent_" + subagentIdCounter.incrementAndGet();

        try {
            // Convert ClaudeAgentOptions to ClaudeCodeOptions if needed
            ClaudeCodeOptions codeOptions = (options instanceof ClaudeCodeOptions)
                ? (ClaudeCodeOptions) options
                : convertToCodeOptions((ClaudeAgentOptions) options);

            Subagent subagent = new Subagent(subagentId, type, config, processManager, codeOptions);
            subagent.start();

            activeSubagents.put(subagentId, subagent);
            logger.info("启动子代理: {} (类型: {})", subagentId, type);

            return subagentId;
        } catch (Exception e) {
            logger.error("启动子代理失败: {}", subagentId, e);
            throw new ClaudeCodeException("SUBAGENT_START_ERROR", "启动子代理失败", e);
        }
    }

    /**
     * 注册内联子代理定义（v2.0.0 新功能）
     *
     * @param definition 子代理定义
     */
    public void registerInlineAgent(SubagentDefinition definition) {
        inlineAgents.put(definition.getName(), definition);
        logger.info("注册内联子代理: {}", definition.getName());
    }

    /**
     * 启动内联子代理（v2.0.0 新功能）
     *
     * @param agentName 子代理名称
     * @param context 上下文数据
     * @return 子代理ID
     * @throws ClaudeCodeException 如果子代理不存在或启动失败
     */
    public String startInlineSubagent(String agentName, Map<String, Object> context) throws ClaudeCodeException {
        SubagentDefinition definition = inlineAgents.get(agentName);
        if (definition == null) {
            throw new ClaudeCodeException("INLINE_AGENT_NOT_FOUND",
                "内联子代理不存在: " + agentName);
        }

        logger.info("启动内联子代理: {}", agentName);
        return startSubagent(agentName, context);
    }

    /**
     * 获取已注册的内联子代理列表
     *
     * @return 子代理定义列表
     */
    public List<SubagentDefinition> getRegisteredInlineAgents() {
        return List.copyOf(inlineAgents.values());
    }

    public void stopSubagent(String subagentId) throws ClaudeCodeException {
        Subagent subagent = activeSubagents.get(subagentId);
        if (subagent == null) {
            throw new ClaudeCodeException("SUBAGENT_NOT_FOUND", "子代理不存在: " + subagentId);
        }

        try {
            subagent.stop();
            activeSubagents.remove(subagentId);
            logger.info("停止子代理: {}", subagentId);
        } catch (Exception e) {
            logger.error("停止子代理失败: {}", subagentId, e);
            throw new ClaudeCodeException("SUBAGENT_STOP_ERROR", "停止子代理失败", e);
        }
    }

    public Subagent getSubagent(String subagentId) {
        return activeSubagents.get(subagentId);
    }

    public List<String> getActiveSubagentIds() {
        return activeSubagents.keySet().stream().collect(Collectors.toList());
    }

    public int getActiveSubagentCount() {
        return activeSubagents.size();
    }

    public void shutdown() {
        logger.info("关闭SubagentManager...");

        for (String subagentId : getActiveSubagentIds()) {
            try {
                stopSubagent(subagentId);
            } catch (Exception e) {
                logger.error("关闭子代理时出错: {}", subagentId, e);
            }
        }

        logger.info("SubagentManager已关闭");
    }

    /**
     * 将 ClaudeAgentOptions 转换为 ClaudeCodeOptions（内部使用）
     *
     * @param agentOptions v2.0.0 配置
     * @return v1.0.0 配置
     */
    private ClaudeCodeOptions convertToCodeOptions(ClaudeAgentOptions agentOptions) {
        return ClaudeCodeOptions.builder()
            .apiKey(agentOptions.getApiKey())
            .baseUrl(agentOptions.getBaseUrl())
            .cliPath(agentOptions.getCliPath())
            .cliEnabled(agentOptions.isCliEnabled())
            .timeout(agentOptions.getTimeout())
            .maxRetries(agentOptions.getMaxRetries())
            .enableLogging(agentOptions.isEnableLogging())
            .environment(agentOptions.getEnvironment())
            .cliMode(agentOptions.getCliMode())
            .ptyReadyTimeout(agentOptions.getPtyReadyTimeout())
            .promptPattern(agentOptions.getPromptPattern())
            .additionalArgs(agentOptions.getAdditionalArgs())
            .minPoolSize(agentOptions.getMinPoolSize())
            .maxPoolSize(agentOptions.getMaxPoolSize())
            .connectionTimeout(agentOptions.getConnectionTimeout())
            .healthCheckInterval(agentOptions.getHealthCheckInterval())
            .authProvider(agentOptions.getAuthProvider())
            .build();
    }
}