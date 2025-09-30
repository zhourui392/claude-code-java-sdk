package com.anthropic.claude.subagents;

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

public class SubagentManager {
    private static final Logger logger = LoggerFactory.getLogger(SubagentManager.class);

    private final ProcessManager processManager;
    private final ClaudeCodeOptions options;
    private final Map<String, Subagent> activeSubagents;
    private final AtomicInteger subagentIdCounter = new AtomicInteger(0);

    public SubagentManager(ProcessManager processManager, ClaudeCodeOptions options) {
        this.processManager = processManager;
        this.options = options;
        this.activeSubagents = new ConcurrentHashMap<>();
    }

    public String startSubagent(String type, Map<String, Object> config) throws ClaudeCodeException {
        String subagentId = "subagent_" + subagentIdCounter.incrementAndGet();

        try {
            Subagent subagent = new Subagent(subagentId, type, config, processManager, options);
            subagent.start();

            activeSubagents.put(subagentId, subagent);
            logger.info("启动子代理: {} (类型: {})", subagentId, type);

            return subagentId;
        } catch (Exception e) {
            logger.error("启动子代理失败: {}", subagentId, e);
            throw new ClaudeCodeException("SUBAGENT_START_ERROR", "启动子代理失败", e);
        }
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
}