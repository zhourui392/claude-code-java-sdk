package com.anthropic.claude.examples;

import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class BaseUrlExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseUrlExample.class);

    private BaseUrlExample() {
        // 工具类不应该实例化
    }

    public static void main(String[] args) {
        try {
            // 方法1: 通过环境变量设置 Base URL
            // export ANTHROPIC_BASE_URL=https://api.packycode.com
            // export ANTHROPIC_AUTH_TOKEN=sk-FETxbAgrd4facvTzrrzHlW2cDrv1ThJQ
            LOGGER.info("=== 方法1: 使用环境变量配置 ===");
            ClaudeCodeSDK sdk1 = new ClaudeCodeSDK();
            LOGGER.info("默认SDK配置: {}", sdk1.getConfiguration());

            // 方法2: 通过代码直接设置 Base URL
            LOGGER.info("=== 方法2: 通过代码配置 ===");
            ClaudeCodeOptions options = ClaudeCodeOptions.builder()
                    .apiKey("sk-FETxbAgrd4facvTzrrzHlW2cDrv1ThJQ")
                    .baseUrl("https://api.packycode.com")
                    .cliPath("claude-code")
                    .timeout(Duration.ofMinutes(5))
                    .maxRetries(3)
                    .enableLogging(true)
                    .build();

            ClaudeCodeSDK sdk2 = new ClaudeCodeSDK(options);
            LOGGER.info("自定义SDK配置: {}", sdk2.getConfiguration());

            // 方法3: 通过配置文件设置
            LOGGER.info("=== 方法3: 配置文件示例 ===");
            LOGGER.info("在 ~/.claude/config.properties 或 claude-code.properties 中添加:");
            LOGGER.info("api.key=sk-FETxbAgrd4facvTzrrzHlW2cDrv1ThJQ");
            LOGGER.info("base.url=https://api.packycode.com");

            // 健康检查
            LOGGER.info("=== 健康检查 ===");
            LOGGER.info("SDK1 认证状态: {}", sdk1.isAuthenticated());
            LOGGER.info("SDK2 认证状态: {}", sdk2.isAuthenticated());

            // 清理资源
            sdk1.shutdown();
            sdk2.shutdown();

        } catch (Exception e) {
            LOGGER.error("运行示例时出错", e);
        }
    }
}