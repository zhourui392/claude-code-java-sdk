# Claude Code Java SDK - 技术规格说明书

## 1. 项目概述

### 1.1 项目背景
- **目标**: 创建Claude Code的Java SDK，通过1:1翻译官方Claude Code Python SDK实现100%功能对等
- **策略**: 直接翻译Anthropic官方Claude Code Python SDK (`claude-code-sdk`)，保持API设计和功能完全一致
- **优势**: 避免重新实现，确保功能完整性和版本同步，为Java生态提供原生Claude Code集成能力

### 1.2 核心价值
- ✅ **100%功能对等**: 与官方Claude Code Python SDK完全一致
- ✅ **快速上线**: 1-2周开发周期 vs 6-8周重新实现
- ✅ **自动更新**: 跟随官方Claude Code SDK版本演进
- ✅ **低维护成本**: 无需独立维护复杂算法
- ✅ **Java生态集成**: 为Java开发者提供原生Claude Code编程能力

## 2. 技术架构

### 2.1 架构原则
```
Claude Code Python SDK     →     1:1翻译     →     Claude Code Java SDK
(claude-code-sdk)                                   (claude-code-java-sdk)
     ↓                                                      ↓
query()异步迭代器                              CompletableFuture/Observable
CLI进程管理                                    ProcessManager (ZT-Exec)
JSON流式解析                                   Jackson流式解析
配置管理                                       ConfigLoader
Hook系统                                       HookService
自定义工具(MCP)                                MCPServer + @Tool注解
```

### 2.1.1 与Python SDK的对应关系
本项目是 **Claude Code Python SDK** (`claude-code-sdk`) 的Java实现，而非Anthropic Messages API的SDK。

**重要区别**:
- **Claude Code SDK**: 基于Claude Code CLI的高级编程接口，提供上下文管理、工具调用、Hook系统等
- **Anthropic API SDK**: 直接调用Anthropic REST API的底层客户端

本项目实现的功能特性：
- ✅ `query()` 异步查询功能（对应Python SDK的async iterator）
- ✅ 自定义工具系统（MCP in-process server）
- ✅ Hook生命周期管理（pre_query, post_query, query_error）
- ✅ 上下文自动压缩和管理
- ✅ 子代理管理（Subagent management）
- ✅ 配置多源加载（环境变量、配置文件、代码配置）

### 2.2 核心架构设计

#### 2.2.1 分层架构
```
┌─────────────────────────────────────┐
│ Application Layer (应用层)           │
│ ├─ ClaudeCodeSDK (主入口)            │
│ ├─ QueryBuilder (查询构建器)         │
│ └─ ConfigurationManager (配置管理)   │
├─────────────────────────────────────┤
│ Service Layer (服务层)               │
│ ├─ QueryService (查询服务)           │
│ ├─ HookService (钩子服务)            │
│ ├─ SubagentService (子代理服务)      │
│ └─ ContextService (上下文服务)       │
├─────────────────────────────────────┤
│ Execution Layer (执行层)             │
│ ├─ ProcessManager (进程管理器)       │
│ ├─ MessageParser (消息解析器)        │
│ └─ StreamHandler (流处理器)          │
├─────────────────────────────────────┤
│ Infrastructure Layer (基础设施层)    │
│ ├─ AuthenticationProvider (认证)     │
│ ├─ ConfigLoader (配置加载器)         │
│ └─ ErrorHandler (错误处理器)         │
└─────────────────────────────────────┘
```

#### 2.2.2 核心模块对应关系

本项目与 **Claude Code Python SDK** 的模块对应关系：

| Claude Code Python SDK | Claude Code Java SDK | 说明 |
|----------------------|---------------------|------|
| `query()` 函数 | `ClaudeCodeSDK.query()` | 异步查询入口 |
| `AsyncIterator<Message>` | `Observable<Message>` / `Stream<Message>` | 流式响应处理 |
| 自定义工具 (Python函数) | `@Tool` 注解 + `MCPServer` | MCP in-process server |
| Hook系统 | `HookService` + `HookCallback` | 生命周期事件管理 |
| 配置管理 | `ConfigLoader` + `ClaudeCodeOptions` | 多源配置加载 |
| 消息模型 | `Message` + `MessageParser` | JSON消息解析 |
| 子代理 | `SubagentManager` + `Subagent` | 长运行子进程管理 |
| CLI进程管理 | `ProcessManager` (基于ZT-Exec) | 跨平台进程执行 |
| 上下文管理 | `ContextManager` + `ContextCompressor` | 智能上下文压缩 |

**Python SDK示例**:
```python
from claude_code_sdk import query

async for message in query(prompt="What is 2 + 2?"):
    print(message)
```

**对应的Java SDK实现**:
```java
ClaudeCodeSDK sdk = new ClaudeCodeSDK();
sdk.queryStream("What is 2 + 2?")
    .subscribe(message -> System.out.println(message));
```

### 2.3 依赖技术选型

#### 2.3.1 核心依赖
```xml
<dependencies>
    <!-- HTTP客户端 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- JSON处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.16.1</version>
    </dependency>

    <!-- 响应式流 -->
    <dependency>
        <groupId>io.reactivex.rxjava3</groupId>
        <artifactId>rxjava</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- 进程管理 -->
    <dependency>
        <groupId>org.zeroturnaround</groupId>
        <artifactId>zt-exec</artifactId>
        <version>1.12</version>
    </dependency>

    <!-- 缓存 -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- 配置管理 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>6.1.0</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### 2.3.2 环境要求
- **JDK版本**: Java 17+
- **构建工具**: Maven 3.8+ 或 Gradle 7.0+
- **依赖环境**: Claude Code CLI 需要预先安装
- **操作系统**: Windows 11, macOS 12+, Linux (Ubuntu 20.04+)

## 3. API设计

### 3.1 核心API接口

#### 3.1.1 主客户端接口
```java
public class ClaudeCodeSDK {
    // 构造函数
    public ClaudeCodeSDK()
    public ClaudeCodeSDK(ClaudeCodeOptions options)

    // 核心查询方法
    public CompletableFuture<Stream<Message>> query(String prompt)
    public CompletableFuture<Stream<Message>> query(QueryRequest request)

    // 流式查询
    public Observable<Message> queryStream(String prompt)
    public Observable<Message> queryStream(QueryRequest request)

    // Hook管理
    public void addHook(String eventType, HookCallback callback)
    public void removeHook(String eventType, HookCallback callback)

    // 子代理管理
    public SubagentManager getSubagentManager()

    // 配置管理
    public void configure(ClaudeCodeOptions options)
    public ClaudeCodeOptions getConfiguration()
}
```

#### 3.1.2 查询构建器
```java
public class QueryBuilder {
    public QueryBuilder withPrompt(String prompt)
    public QueryBuilder withTools(String... tools)
    public QueryBuilder withContext(String context)
    public QueryBuilder withMaxTokens(int maxTokens)
    public QueryBuilder withTemperature(double temperature)
    public QueryBuilder withTimeout(Duration timeout)

    // 终端操作
    public CompletableFuture<String> execute()
    public CompletableFuture<Stream<Message>> stream()
    public Observable<Message> observe()
}
```

#### 3.1.3 消息模型
```java
public class Message {
    private String id;
    private MessageType type;
    private String content;
    private Map<String, Object> metadata;
    private Instant timestamp;

    // 静态工厂方法
    public static Message fromJson(String json)
    public static Message text(String content)
    public static Message tool(String toolName, Map<String, Object> args)
}

public enum MessageType {
    TEXT, TOOL_CALL, TOOL_RESULT, ERROR, SYSTEM
}
```

### 3.2 配置接口
```java
public class ClaudeCodeOptions {
    private String apiKey;
    private String cliPath;
    private Duration timeout;
    private int maxRetries;
    private boolean enableLogging;
    private Map<String, String> environment;
    private AuthenticationProvider authProvider;

    // Builder模式
    public static class Builder {
        public Builder apiKey(String apiKey)
        public Builder cliPath(String cliPath)
        public Builder timeout(Duration timeout)
        public Builder maxRetries(int maxRetries)
        public Builder enableLogging(boolean enableLogging)
        public Builder environment(Map<String, String> env)
        public Builder authProvider(AuthenticationProvider provider)
        public ClaudeCodeOptions build()
    }
}
```

### 3.3 Hook系统接口
```java
@FunctionalInterface
public interface HookCallback {
    HookResult execute(HookContext context);
}

public class HookContext {
    private String eventType;
    private Map<String, Object> data;
    private Instant timestamp;
    private String sessionId;
}

public class HookResult {
    private boolean shouldContinue;
    private Map<String, Object> modifiedData;
    private String message;

    public static HookResult proceed()
    public static HookResult proceed(Map<String, Object> modifiedData)
    public static HookResult stop(String reason)
}
```

## 4. 实现要求

### 4.1 功能要求
- ✅ **基础查询**: 支持文本提示查询，流式响应
- ✅ **工具调用**: 支持Read, Write, Edit等工具
- ✅ **Hook系统**: 支持pre/post事件钩子
- ✅ **子代理**: 支持子代理启动和管理
- ✅ **上下文管理**: 自动上下文压缩和管理
- ✅ **配置管理**: 支持环境变量、配置文件
- ✅ **认证支持**: 支持API Key、第三方提供商认证

### 4.2 性能要求
- **响应时间**: P95 < 2秒 (不含模型推理时间)
- **吞吐量**: 支持100并发请求
- **内存使用**: 堆内存 < 512MB
- **CPU使用**: 正常情况下 < 10%

### 4.3 可靠性要求
- **可用性**: 99.9%
- **错误恢复**: 自动重试机制，指数退避
- **熔断保护**: 防止级联故障
- **超时控制**: 可配置的请求超时

### 4.4 兼容性要求
- **平台兼容**: Windows 11, macOS 12+, Linux
- **JDK兼容**: Java 17, 21, 23
- **CLI兼容**: 跟随Claude Code CLI版本

## 5. 安全要求

### 5.1 认证安全
- API密钥通过环境变量或安全配置文件管理
- 支持密钥轮换机制
- 不在日志中记录敏感信息

### 5.2 进程安全
- CLI进程隔离执行
- 输入参数严格验证
- 防止命令注入攻击

### 5.3 网络安全
- 强制使用HTTPS/TLS 1.2+
- 证书验证
- 支持代理配置

## 6. 测试要求

### 6.1 单元测试
- 代码覆盖率 > 85%
- 核心模块覆盖率 > 95%
- Mock CLI调用进行测试

### 6.2 集成测试
- 与实际CLI集成测试
- 多线程并发测试
- 异常场景测试

### 6.3 性能测试
- 负载测试：100并发用户
- 压力测试：确定系统极限
- 内存泄漏测试

## 7. 部署和运维

### 7.1 打包发布
- Maven Central仓库发布
- 支持Spring Boot Starter
- 提供Docker镜像

### 7.2 监控指标
- API调用成功率
- 平均响应时间
- CLI进程状态
- 内存使用情况

### 7.3 日志管理
- 结构化日志输出
- 可配置日志级别
- 支持日志聚合系统

## 8. 项目交付

### 8.1 交付物
- [ ] 源代码 (GitHub仓库)
- [ ] API文档 (JavaDoc + Markdown)
- [ ] 使用示例 (Examples目录)
- [ ] 测试报告
- [ ] 部署指南

### 8.2 质量标准
- 代码符合Alibaba P3C规范
- API设计符合Java最佳实践
- 完整的错误处理和异常管理
- 详细的文档和示例

### 8.3 维护计划
- 跟随Claude Code CLI版本更新
- 定期安全更新
- 社区Issue响应机制
- 长期技术支持计划

---

**文档版本**: v1.0
**创建日期**: 2025-09-14
**更新日期**: 2025-09-14
**负责人**: 开发团队