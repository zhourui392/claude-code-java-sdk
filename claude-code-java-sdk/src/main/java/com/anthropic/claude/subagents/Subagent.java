package com.anthropic.claude.subagents;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Subagent {
    private static final Logger logger = LoggerFactory.getLogger(Subagent.class);

    private final String id;
    private final String type;
    private final Map<String, Object> config;
    private final ProcessManager processManager;
    private final ClaudeCodeOptions options;

    private volatile boolean isRunning = false;

    public Subagent(String id, String type, Map<String, Object> config,
                   ProcessManager processManager, ClaudeCodeOptions options) {
        this.id = id;
        this.type = type;
        this.config = config;
        this.processManager = processManager;
        this.options = options;
    }

    public void start() {
        if (isRunning) {
            logger.warn("子代理已在运行: {}", id);
            return;
        }

        logger.info("启动子代理: {} (类型: {})", id, type);
        isRunning = true;
    }

    public void stop() {
        if (!isRunning) {
            logger.warn("子代理未在运行: {}", id);
            return;
        }

        logger.info("停止子代理: {}", id);
        isRunning = false;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public boolean isRunning() {
        return isRunning;
    }
}