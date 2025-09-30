package com.anthropic.claude.strategy;

import com.anthropic.claude.config.CliMode;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.messages.MessageParser;
import com.anthropic.claude.process.ProcessManager;
import com.anthropic.claude.pty.PtyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CLI执行策略工厂
 *
 * 根据配置创建相应的CLI执行策略
 *
 * @author Claude Code SDK
 */
public class CliExecutionStrategyFactory {
    private static final Logger logger = LoggerFactory.getLogger(CliExecutionStrategyFactory.class);

    /**
     * 创建CLI执行策略
     *
     * @param options 配置选项
     * @param processManager 进程管理器
     * @param ptyManager PTY管理器
     * @param messageParser 消息解析器
     * @return CLI执行策略
     * @throws ClaudeCodeException 创建失败时抛出
     */
    public static CliExecutionStrategy createStrategy(ClaudeCodeOptions options,
                                                     ProcessManager processManager,
                                                     PtyManager ptyManager,
                                                     MessageParser messageParser) throws ClaudeCodeException {

        CliMode mode = options.getCliMode();
        logger.info("创建CLI执行策略: {}", mode.getDescription());

        switch (mode) {
            case BATCH:
                return createBatchStrategy(options, processManager, messageParser);

            case PTY_INTERACTIVE:
                return createPtyInteractiveStrategy(options, processManager, ptyManager, messageParser);

            default:
                throw new ClaudeCodeException("不支持的CLI模式: " + mode);
        }
    }

    /**
     * 创建批处理策略
     */
    private static BatchProcessStrategy createBatchStrategy(ClaudeCodeOptions options,
                                                           ProcessManager processManager,
                                                           MessageParser messageParser) {
        logger.debug("创建批处理策略");
        return new BatchProcessStrategy(processManager, messageParser, options);
    }

    /**
     * 创建PTY交互策略
     */
    private static PtyInteractiveStrategy createPtyInteractiveStrategy(ClaudeCodeOptions options,
                                                                      ProcessManager processManager,
                                                                      PtyManager ptyManager,
                                                                      MessageParser messageParser) {
        logger.debug("创建PTY交互策略");

        // 创建回退策略
        BatchProcessStrategy fallbackStrategy = createBatchStrategy(options, processManager, messageParser);

        return new PtyInteractiveStrategy(ptyManager, messageParser, options, fallbackStrategy);
    }

    /**
     * 创建默认策略（批处理模式）
     */
    public static CliExecutionStrategy createDefaultStrategy(ProcessManager processManager,
                                                            MessageParser messageParser,
                                                            ClaudeCodeOptions options) {
        logger.info("创建默认CLI执行策略（批处理模式）");
        return createBatchStrategy(options, processManager, messageParser);
    }
}