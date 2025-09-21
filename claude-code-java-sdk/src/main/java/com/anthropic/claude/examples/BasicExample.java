package com.anthropic.claude.examples;

import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.hooks.HookResult;
import com.anthropic.claude.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.stream.Stream;

public class BasicExample {
    private static final Logger logger = LoggerFactory.getLogger(BasicExample.class);

    public static void main(String[] args) {
        try {
            // 创建配置选项
            ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                    .apiKey("your-api-key-here")
                    .cliPath("claude-code")
                    .timeout(Duration.ofMinutes(5))
                    .maxRetries(3)
                    .enableLogging(true)
                    .build();

            // 创建SDK实例
            ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);

            // 检查CLI是否可用
            if (!sdk.isCliAvailable()) {
                logger.error("Claude Code CLI 不可用，请确保已正确安装");
                return;
            }

            // 添加Hook示例
            sdk.addHook("pre_query", context -> {
                logger.info("准备执行查询: {}", context.getData("prompt"));
                return HookResult.proceed();
            });

            sdk.addHook("post_query", context -> {
                Object messages = context.getData("messages");
                logger.info("查询完成，收到响应: {}", messages);
                return HookResult.proceed();
            });

            // 执行基础查询
            String prompt = "请帮我写一个Java的Hello World程序";
            Stream<Message> messages = sdk.query(prompt).join();

            // 处理响应消息
            messages.forEach(message -> {
                logger.info("消息类型: {}, 内容: {}", message.getType(), message.getContent());
            });

            // 使用QueryBuilder进行更复杂的查询
            sdk.queryBuilder("分析这段代码的性能问题")
                    .withTools("Read", "Edit")
                    .withMaxTokens(1000)
                    .withTemperature(0.7)
                    .withTimeout(Duration.ofMinutes(3))
                    .execute()
                    .thenAccept(result -> {
                        logger.info("分析结果: {}", result);
                    })
                    .join();

            // 使用流式查询
            sdk.queryStream("请解释Java中的垃圾回收机制")
                    .subscribe(
                            message -> logger.info("流式消息: {}", message.getContent()),
                            throwable -> logger.error("流式查询出错", throwable),
                            () -> logger.info("流式查询完成")
                    );

            // 关闭SDK
            sdk.shutdown();

        } catch (Exception e) {
            logger.error("示例运行出错", e);
        }
    }
}