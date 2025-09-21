# Claude Code Java SDK

Claude Code的官方Java SDK实现，通过1:1翻译Python SDK确保100%功能对等。

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.8+ 或 Gradle 7.0+
- Claude Code CLI（需要预先安装）

### 安装

#### Maven

```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>claude-code-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle

```gradle
implementation 'com.anthropic:claude-code-java-sdk:1.0.0'
```

### 基础使用

```java
import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.messages.Message;

// 创建SDK实例
ClaudeCodeSDK sdk = new ClaudeCodeSDK();

// 执行查询
Stream<Message> messages = sdk.query("请帮我写一个Java Hello World程序").join();

// 处理响应
messages.forEach(message -> {
    System.out.println("类型: " + message.getType());
    System.out.println("内容: " + message.getContent());
});
```

## 📚 核心功能

### 1. 基础查询

```java
// 简单文本查询
sdk.query("解释Java中的多态性").join();

// 使用QueryRequest
QueryRequest request = QueryRequest.builder("分析代码性能")
    .withTools("Read", "Edit")
    .withMaxTokens(1000)
    .withTemperature(0.7)
    .build();

sdk.query(request).join();
```

### 2. 流式响应

```java
// 使用RxJava Observable
sdk.queryStream("解释机器学习基础概念")
    .subscribe(
        message -> System.out.println("收到: " + message.getContent()),
        throwable -> System.err.println("错误: " + throwable.getMessage()),
        () -> System.out.println("完成")
    );
```

### 3. QueryBuilder模式

```java
sdk.queryBuilder("优化这段代码")
    .withTools("Read", "Write", "Edit")
    .withContext("这是一个性能关键的函数")
    .withMaxTokens(2000)
    .withTimeout(Duration.ofMinutes(5))
    .execute()
    .thenAccept(result -> System.out.println(result));
```

### 4. Hook系统

```java
// 添加查询前Hook
sdk.addHook("pre_query", context -> {
    System.out.println("准备查询: " + context.getData("prompt"));
    return HookResult.proceed();
});

// 添加查询后Hook
sdk.addHook("post_query", context -> {
    System.out.println("查询完成");
    return HookResult.proceed();
});

// 添加错误处理Hook
sdk.addHook("query_error", context -> {
    System.err.println("查询失败: " + context.getData("error"));
    return HookResult.proceed();
});
```

### 5. 子代理管理 

```java
SubagentManager manager = sdk.getSubagentManager();

// 启动子代理
String agentId = manager.startSubagent("analysis", Map.of(
    "specialization", "code_review",
    "timeout", 300
));

// 获取子代理状态
Subagent agent = manager.getSubagent(agentId);
System.out.println("代理状态: " + agent.isRunning());

// 停止子代理
manager.stopSubagent(agentId);
```

## ⚙️ 配置管理

### 环境变量配置

```bash
# 设置API密钥
export ANTHROPIC_API_KEY="your-api-key"

# 设置CLI路径
export CLAUDE_CODE_CLI_PATH="/usr/local/bin/claude-code"

# 设置超时时间（秒）
export CLAUDE_CODE_TIMEOUT_SECONDS="600"

# 设置最大重试次数
export CLAUDE_CODE_MAX_RETRIES="3"
```

### 配置文件

在用户目录下创建 `~/.claude/config.properties`:

```properties
# API配置
api.key=your-api-key-here

# CLI配置
cli.path=claude-code
timeout.seconds=600
max.retries=3

# 日志配置
logging.enabled=true

# 环境变量
env.claude_project_name=my-project
env.claude_environment=production
```

### 代码配置

```java
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .apiKey("your-api-key")
    .cliPath("claude-code")
    .timeout(Duration.ofMinutes(10))
    .maxRetries(3)
    .enableLogging(true)
    .addEnvironment("CLAUDE_PROJECT", "my-project")
    .build();

ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);
```

## 🧪 错误处理

```java
try {
    Stream<Message> messages = sdk.query("分析代码").join();
    // 处理正常响应
} catch (ClaudeCodeException e) {
    switch (e.getErrorCode()) {
        case "QUERY_EXECUTION_ERROR":
            System.err.println("查询执行失败: " + e.getMessage());
            break;
        case "CONFIG_VALIDATION_ERROR":
            System.err.println("配置错误: " + e.getMessage());
            break;
        case "PROCESS_EXECUTION_ERROR":
            System.err.println("进程执行错误: " + e.getMessage());
            break;
        default:
            System.err.println("未知错误: " + e.getMessage());
    }
}
```

## 🔧 高级功能

### 自定义认证提供者

```java
AuthenticationProvider customAuth = new AuthenticationProvider() {
    @Override
    public String getApiKey() {
        return getKeyFromSecureStorage();
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", getApiKey());
        headers.put("x-custom-header", "custom-value");
        return headers;
    }

    @Override
    public boolean isAuthenticated() {
        return getApiKey() != null;
    }

    @Override
    public void refreshAuth() {
        // 刷新认证逻辑
    }
};

ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .authProvider(customAuth)
    .build();
```

### 健康检查

```java
// SDK健康检查
if (sdk.healthCheck()) {
    System.out.println("SDK运行正常");
} else {
    System.out.println("SDK存在问题");
}

// 检查CLI可用性
if (sdk.isCliAvailable()) {
    System.out.println("CLI可用");
}

// 检查认证状态
if (sdk.isAuthenticated()) {
    System.out.println("认证成功");
}
```

## 📊 监控和日志

SDK使用SLF4J进行日志记录，你可以配置不同的日志实现：

### Logback配置示例

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.anthropic.claude" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

## 🚀 性能优化

### 连接池配置

```java
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .timeout(Duration.ofMinutes(5))  // 适当的超时时间
    .maxRetries(3)                   // 合理的重试次数
    .build();
```

### 批量查询

```java
List<CompletableFuture<Stream<Message>>> futures = prompts.stream()
    .map(sdk::query)
    .toList();

// 等待所有查询完成
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenRun(() -> {
        futures.forEach(future -> {
            // 处理每个查询结果
            future.join().forEach(this::processMessage);
        });
    });
```

## 🔐 安全最佳实践

1. **API密钥管理**
   - 使用环境变量存储API密钥
   - 不要在代码中硬编码密钥
   - 定期轮换API密钥

2. **输入验证**
   - 验证和清理用户输入
   - 防止命令注入攻击

3. **日志安全**
   - 不要在日志中记录敏感信息
   - 使用脱敏处理

## 🤝 贡献指南

欢迎贡献！请阅读我们的[贡献指南](CONTRIBUTING.md)了解详情。

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🆘 支持

- 📖 [官方文档](https://docs.anthropic.com/claude-code)
- 🐛 [问题反馈](https://github.com/anthropics/claude-code-java-sdk/issues)
- 💬 [社区讨论](https://github.com/anthropics/claude-code-java-sdk/discussions)

## 🗺️ 路线图

- [ ] Spring Boot Starter
- [ ] Metrics和监控集成
- [ ] 更多示例和文档
- [ ] 性能优化
- [ ] 企业级功能