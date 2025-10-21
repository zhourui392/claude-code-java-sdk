# 迁移指南：v1.0.0 → v2.0.0

> **目标版本**: Claude Agent SDK Java v2.0.0
> **源版本**: Claude Code SDK Java v1.0.0
> **预计迁移时间**: 15-30 分钟（小型项目）到 2-4 小时（大型项目）

---

## 🎯 概述

v2.0.0 是一个重大更新，将项目从 **Claude Code SDK** 重命名为 **Claude Agent SDK**，以对齐 Anthropic 官方命名。同时引入了 5 个新特性：

1. Session Forking（会话分支）
2. Programmatic Subagents（编程式子代理）
3. Settings Sources Configuration（设置源配置）
4. Custom Transport（自定义传输层）
5. Enhanced Hook Fields（增强钩子字段）

**好消息**: v2.0.0 提供完整的向后兼容层，旧代码仍可运行，但会收到弃用警告。

---

## 📊 迁移速查表

| v1.0.0 (旧) | v2.0.0 (新) | 状态 | 移除版本 |
|-------------|-------------|------|----------|
| `ClaudeCodeSDK` | `ClaudeAgentSDK` | @Deprecated | v3.0.0 |
| `ClaudeCodeOptions` | `ClaudeAgentOptions` | @Deprecated | v3.0.0 |
| `ClaudeCodeException` | `ClaudeAgentException` | @Deprecated | v3.0.0 |
| `HookResult.getMessage()` | `HookResult.getReason()` | @Deprecated | v3.0.0 |
| `HookResult.shouldContinue` | `HookResult.shouldContinue()` | 保持兼容 | - |

---

## 🚀 快速迁移（5 步）

### Step 1: 更新依赖

**Maven (pom.xml)**:
```xml
<!-- 旧版本 -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>claude-code-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 新版本 -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>claude-agent-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Gradle (build.gradle)**:
```gradle
// 旧版本
implementation 'com.anthropic:claude-code-java-sdk:1.0.0'

// 新版本
implementation 'com.anthropic:claude-agent-java-sdk:2.0.0'
```

### Step 2: 更新 Import 语句

**查找并替换**（推荐使用 IDE 的全局替换功能）:

```java
// 旧 imports
import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;

// 新 imports
import com.anthropic.claude.client.ClaudeAgentSDK;
import com.anthropic.claude.config.ClaudeAgentOptions;
import com.anthropic.claude.exceptions.ClaudeAgentException;
```

### Step 3: 更新类名

**自动替换模式**（在 IDE 中使用 Refactor → Rename）:

```
ClaudeCodeSDK → ClaudeAgentSDK
ClaudeCodeOptions → ClaudeAgentOptions
ClaudeCodeException → ClaudeAgentException
```

### Step 4: 更新异常捕获

```java
// 旧代码
try {
    sdk.query("test").join();
} catch (ClaudeCodeException e) {
    // 处理异常
}

// 新代码
try {
    sdk.query("test").join();
} catch (ClaudeAgentException e) {
    // 处理异常
}
```

### Step 5: 运行测试

```bash
mvn clean test
```

如果有弃用警告，参考下文的详细迁移指南逐一修复。

---

## 📖 详细迁移指南

### 1. 基本 SDK 初始化

#### Before (v1.0.0)
```java
import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;

ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .timeout(Duration.ofMinutes(10))
    .build();

ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);
```

#### After (v2.0.0)
```java
import com.anthropic.claude.client.ClaudeAgentSDK;
import com.anthropic.claude.config.ClaudeAgentOptions;

ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .timeout(Duration.ofMinutes(10))
    .build();

ClaudeAgentSDK sdk = new ClaudeAgentSDK(options);
```

**变更点**:
- 类名从 `ClaudeCodeSDK` → `ClaudeAgentSDK`
- 配置类从 `ClaudeCodeOptions` → `ClaudeAgentOptions`

---

### 2. 查询执行

#### Before (v1.0.0)
```java
// 同步查询
CompletableFuture<Stream<Message>> future = sdk.query("What is 2+2?");
Stream<Message> messages = future.join();
messages.forEach(System.out::println);

// 流式查询
Observable<Message> stream = sdk.queryStream("What is 2+2?");
stream.subscribe(message -> System.out.println(message));
```

#### After (v2.0.0)
```java
// 完全相同，无需修改！
CompletableFuture<Stream<Message>> future = sdk.query("What is 2+2?");
Stream<Message> messages = future.join();
messages.forEach(System.out::println);

Observable<Message> stream = sdk.queryStream("What is 2+2?");
stream.subscribe(message -> System.out.println(message));
```

**变更点**: ✅ 无变更，API 100% 兼容

---

### 3. Hook 系统

#### Before (v1.0.0)
```java
sdk.addHook("pre_query", context -> {
    System.out.println("查询前: " + context.getData().get("prompt"));

    return HookResult.builder()
        .shouldContinue(true)
        .message("继续执行")
        .build();
});
```

#### After (v2.0.0) - 推荐新 API
```java
sdk.addHook("pre_query", context -> {
    System.out.println("查询前: " + context.getData().get("prompt"));

    // 使用新字段
    return HookResult.builder()
        .decision(HookResult.HookDecision.APPROVE)  // 新增
        .reason("继续执行")                           // 重命名自 message
        .continue_(true)                             // 重命名自 shouldContinue
        .build();
});

// 或使用快捷方法
sdk.addHook("pre_query", context -> HookResult.approve());
```

**变更点**:
- 新增 `decision` 字段（APPROVE/BLOCK）
- `message` → `reason`（仍支持 `getMessage()` 兼容方法）
- `shouldContinue` → `continue_`（仍支持 `shouldContinue()` 兼容方法）
- 新增快捷方法：`approve()`, `block()`, `suppress()`

**兼容性**: v1.0.0 代码仍可运行，但建议迁移到新 API

---

### 4. 异常处理

#### Before (v1.0.0)
```java
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.exceptions.ProcessExecutionException;

try {
    sdk.query("test").join();
} catch (ClaudeCodeException e) {
    logger.error("Claude 错误", e);
} catch (ProcessExecutionException e) {
    logger.error("进程执行错误", e);
}
```

#### After (v2.0.0)
```java
import com.anthropic.claude.exceptions.ClaudeAgentException;
import com.anthropic.claude.exceptions.ProcessExecutionException;

try {
    sdk.query("test").join();
} catch (ClaudeAgentException e) {  // 唯一变更
    logger.error("Claude 错误", e);
} catch (ProcessExecutionException e) {
    logger.error("进程执行错误", e);
}
```

**变更点**:
- `ClaudeCodeException` → `ClaudeAgentException`
- 子异常类（ProcessExecutionException 等）无变更

**兼容性**: `ClaudeCodeException` 现在继承自 `ClaudeAgentException`，旧代码仍可捕获

---

### 5. 配置源管理（新功能）

#### Before (v1.0.0)
```java
// v1.0.0 自动加载所有配置源：
// - 默认值
// - 环境变量
// - ~/.claude/config.properties
// - CLAUDE.md
// - Slash commands
// 无法自定义

ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .apiKey("...")
    .build();
```

#### After (v2.0.0) - 新增控制
```java
import com.anthropic.claude.config.SettingSource;

// 方式 1: 使用新默认行为（只加载 env + user_config + inline）
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey("...")
    .build();

// 方式 2: 自定义配置源
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey("...")
    .settingSources(
        SettingSource.ENV_VARS,
        SettingSource.USER_CONFIG,
        SettingSource.INLINE_OPTIONS
    )
    .build();

// 方式 3: 保持 v1.0.0 行为（加载所有源）
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey("...")
    .useV1CompatibilityMode()  // ⚠️ 加载所有源
    .build();
```

**Breaking Change**:
- ⚠️ v2.0.0 默认**不再**自动加载 CLAUDE.md 和 Slash commands
- 如需 v1.x 行为，调用 `.useV1CompatibilityMode()`

**迁移建议**:
- 如果你依赖 CLAUDE.md 或 slash commands → 使用 `.useV1CompatibilityMode()`
- 如果你只用环境变量/配置文件 → 无需修改

---

### 6. Session Forking（新功能）

v1.0.0 不支持会话分支，v2.0.0 新增。

#### After (v2.0.0)
```java
SessionManager sessionManager = sdk.getSessionManager();

// 创建主会话
String mainSession = sessionManager.createSession();

// 在主会话中执行查询
sdk.query(QueryRequest.builder("设计一个 REST API")
    .sessionId(mainSession)
    .build()).join();

// 从主会话创建两个分支，探索不同方案
String fork1 = sessionManager.forkSession(mainSession);
String fork2 = sessionManager.forkSession(mainSession);

// 在 fork1 中探索方案 A
sdk.query(QueryRequest.builder("使用 Spring Boot 实现")
    .sessionId(fork1)
    .build()).join();

// 在 fork2 中探索方案 B
sdk.query(QueryRequest.builder("使用 Quarkus 实现")
    .sessionId(fork2)
    .build()).join();

// 获取会话的所有分支
List<String> forks = sessionManager.getSessionForks(mainSession);
```

**使用场景**:
- A/B 测试不同的解决方案
- 探索多个设计路径
- 保留主会话的同时尝试新想法

---

### 7. Programmatic Subagents（新功能）

v1.0.0 只支持基于类型的外部子代理，v2.0.0 支持内联定义。

#### Before (v1.0.0) - 只能外部定义
```java
// 需要预先配置 subagent 类型
sdk.getSubagentManager().startSubagent("code-reviewer", config);
```

#### After (v2.0.0) - 可内联定义
```java
import com.anthropic.claude.subagents.SubagentDefinition;

// 方式 1: 在配置中定义
SubagentDefinition codeReviewer = SubagentDefinition.builder("code-reviewer")
    .description("Reviews code quality and suggests improvements")
    .systemPrompt("You are an expert Java code reviewer")
    .tools("Read", "Grep", "Glob")
    .model("claude-sonnet-4-5")
    .build();

ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey("...")
    .addAgent(codeReviewer)
    .build();

ClaudeAgentSDK sdk = new ClaudeAgentSDK(options);

// 方式 2: 运行时注册
SubagentDefinition testWriter = SubagentDefinition.builder("test-writer")
    .description("Writes unit tests")
    .systemPrompt("You write excellent JUnit 5 tests")
    .tools("Read", "Write", "Edit")
    .build();

sdk.getSubagentManager().registerInlineAgent(testWriter);

// 启动内联子代理
String agentId = sdk.getSubagentManager()
    .startInlineSubagent("code-reviewer", context);
```

**优势**:
- 无需文件系统依赖
- 动态创建子代理
- 更好的封装和测试

---

### 8. Custom Transport（新功能）

v1.0.0 只支持 CLI 进程，v2.0.0 支持自定义传输层。

#### Before (v1.0.0) - 硬编码 CLI 进程
```java
// 只能通过 ProcessManager 使用 CLI
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .cliPath("/path/to/claude-code")
    .build();
```

#### After (v2.0.0) - 可自定义传输层
```java
import com.anthropic.claude.transport.ClaudeTransport;
import com.anthropic.claude.transport.ProcessTransport;

// 方式 1: 使用默认 CLI 传输（与 v1.0.0 相同）
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .cliPath("/path/to/claude-code")
    .build();

// 方式 2: 使用自定义传输层（新功能）
public class CustomTransport implements ClaudeTransport {
    @Override
    public void send(String message) {
        // 自定义发送逻辑（如 HTTP API）
    }

    @Override
    public Observable<String> receive() {
        // 自定义接收逻辑
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void close() {
        // 清理资源
    }

    @Override
    public TransportType getType() {
        return TransportType.CUSTOM;
    }
}

ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .transport(new CustomTransport())
    .build();
```

**使用场景**:
- 测试时 mock 传输层
- 使用 HTTP API 代替 CLI
- 自定义通信协议

---

### 9. Enhanced Hook Fields（新功能）

#### Before (v1.0.0) - 基础字段
```java
return HookResult.builder()
    .shouldContinue(true)
    .message("继续")
    .modifiedData(Map.of("key", "value"))
    .build();
```

#### After (v2.0.0) - 完整字段
```java
// 1. 阻止操作
return HookResult.block("包含敏感信息");

// 2. 抑制输出
return HookResult.suppress();

// 3. 提前停止并说明原因
return HookResult.builder()
    .stopReason("用户取消")
    .build();

// 4. 批准并修改数据
return HookResult.builder()
    .decision(HookResult.HookDecision.APPROVE)
    .reason("数据已清洗")
    .modifiedData(Map.of("sanitized", true))
    .build();

// 5. 完整控制
return HookResult.builder()
    .decision(HookResult.HookDecision.APPROVE)
    .reason("通过验证")
    .continue_(true)
    .suppressOutput(false)
    .modifiedData(data)
    .build();
```

**新增字段**:
- `decision`: APPROVE 或 BLOCK
- `suppressOutput`: 是否抑制输出
- `stopReason`: 停止原因（自动设置 continue_ = false）

---

## 🔧 常见问题 (FAQ)

### Q1: 我必须立即迁移吗？

**A**: 不必须。v2.0.0 提供完整向后兼容，旧代码仍可运行。但建议在 6 个月内完成迁移，因为 v3.0.0 将移除所有 @Deprecated API。

### Q2: 迁移会影响现有功能吗？

**A**: 核心功能（query、hooks、subagents）100% 兼容。唯一的 Breaking Change 是 `settingSources` 默认值变更，可通过 `.useV1CompatibilityMode()` 恢复旧行为。

### Q3: 如何知道哪些代码需要迁移？

**A**:
1. 编译时会看到 `@Deprecated` 警告
2. 运行时日志会打印弃用警告
3. 使用 IDE 的 "Deprecated API Usage" 检查

### Q4: v2.0.0 有性能变化吗？

**A**: 无显著性能变化。新功能（如 Session Forking）不影响现有代码性能。

### Q5: 如何处理依赖冲突？

**A**:
```xml
<!-- 排除旧版本 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-lib</artifactId>
    <version>1.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.anthropic</groupId>
            <artifactId>claude-code-java-sdk</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 显式声明新版本 -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>claude-agent-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Q6: GUI 模块需要迁移吗？

**A**: 是的，GUI 模块也需要更新导入语句和类名。迁移步骤相同。

### Q7: 测试用例需要修改吗？

**A**: 大部分测试无需修改。只需更新导入语句和类名。如果使用了 `ClaudeCodeException` 捕获，需要改为 `ClaudeAgentException`。

### Q8: 如何验证迁移成功？

**A**:
```bash
# 1. 编译通过且无警告
mvn clean compile -Xlint:deprecation

# 2. 所有测试通过
mvn test

# 3. 运行时无弃用日志
# 检查日志中是否有 "⚠️  ClaudeCodeSDK 已弃用" 等警告
```

---

## 🛠️ 自动化迁移工具（可选）

### 使用 IntelliJ IDEA

1. **全局查找替换**:
   - `Ctrl+Shift+R` (Windows/Linux) 或 `Cmd+Shift+R` (macOS)
   - 勾选 "Match case" 和 "Words"
   - 依次替换：
     ```
     ClaudeCodeSDK → ClaudeAgentSDK
     ClaudeCodeOptions → ClaudeAgentOptions
     ClaudeCodeException → ClaudeAgentException
     ```

2. **检查 Deprecated API**:
   - `Analyze → Inspect Code`
   - 查看 "Deprecated API usage" 警告

3. **优化 Imports**:
   - `Ctrl+Alt+O` (Windows/Linux) 或 `Cmd+Option+O` (macOS)

### 使用 Eclipse

1. **全局查找替换**:
   - `Ctrl+H` → File Search
   - 搜索并替换类名

2. **检查 Deprecated API**:
   - `Project → Properties → Java Compiler → Errors/Warnings`
   - 启用 "Deprecated API" 警告

### Shell 脚本（批量替换）

```bash
#!/bin/bash
# migrate-to-v2.sh

find . -name "*.java" -type f -exec sed -i \
  -e 's/ClaudeCodeSDK/ClaudeAgentSDK/g' \
  -e 's/ClaudeCodeOptions/ClaudeAgentOptions/g' \
  -e 's/ClaudeCodeException/ClaudeAgentException/g' \
  {} +

echo "✅ 迁移完成！请运行 'mvn clean test' 验证。"
```

**使用方法**:
```bash
chmod +x migrate-to-v2.sh
./migrate-to-v2.sh
```

⚠️ **警告**: 在运行脚本前请备份代码或提交到版本控制！

---

## 📋 迁移检查清单

在完成迁移后，使用此清单验证：

- [ ] 更新 Maven/Gradle 依赖到 v2.0.0
- [ ] 替换所有 `ClaudeCodeSDK` → `ClaudeAgentSDK`
- [ ] 替换所有 `ClaudeCodeOptions` → `ClaudeAgentOptions`
- [ ] 替换所有 `ClaudeCodeException` → `ClaudeAgentException`
- [ ] 更新异常捕获代码
- [ ] 检查配置源行为（是否需要 `.useV1CompatibilityMode()`）
- [ ] 运行 `mvn clean compile -Xlint:deprecation`，确认无警告
- [ ] 运行 `mvn test`，确认所有测试通过
- [ ] 检查运行时日志，确认无弃用警告
- [ ] 更新项目文档和 README

**可选**（如果使用新功能）:
- [ ] 尝试 Session Forking 功能
- [ ] 使用 Programmatic Subagents 重构现有子代理
- [ ] 自定义 Settings Sources 配置
- [ ] 更新 Hook 使用新字段（decision、suppressOutput、stopReason）

---

## 📞 获取帮助

如果迁移过程中遇到问题：

1. **查看文档**:
   - [UPGRADE_PLAN_V2.md](UPGRADE_PLAN_V2.md) - 完整升级规划
   - [CHANGELOG.md](CHANGELOG.md) - 详细变更记录
   - [README.md](README.md) - 项目总览

2. **提交 Issue**:
   - GitHub Issues: [项目地址]/issues
   - 标签: `migration`, `v2.0.0`

3. **社区支持**:
   - 讨论区: [项目地址]/discussions
   - Stack Overflow: 标签 `claude-agent-sdk-java`

---

**版本**: v1.0
**最后更新**: 2025-10-21
**适用版本**: v1.0.0 → v2.0.0
