# Python SDK 分析文档

## 概述
基于公开信息，Claude Code Python SDK 提供了以下核心功能：

## 核心功能模块

### 1. 查询接口 (Query Interface)
- **异步查询函数**: `async query(prompt)` - 返回AsyncIterator响应流
- **同步查询包装**: 提供同步API供非异步环境使用
- **流式响应处理**: 支持实时接收和处理响应

### 2. 工具系统 (Tools System)
- **自定义工具注册**: 允许注册Python函数作为Claude可调用的工具
- **MCP服务器集成**: 内置MCP (Model Context Protocol)服务器支持
- **工具发现机制**: 自动发现和注册标记的工具函数

### 3. Hook系统 (Hooks System)
- **生命周期钩子**: 在查询的不同阶段触发
- **事件监听**: 支持监听和响应各种事件
- **中间件支持**: 允许在请求/响应流程中插入自定义逻辑

### 4. 子代理管理 (Subagent Management)
- **子代理启动**: 创建和管理子代理实例
- **任务分发**: 将任务分配给适当的子代理
- **结果聚合**: 收集和整合子代理的执行结果

### 5. 进程管理 (Process Management)
- **CLI调用**: 执行命令行工具
- **异步进程执行**: 非阻塞的进程执行
- **输出流处理**: 实时捕获和处理进程输出

## Java SDK 实现计划

### 第一步：核心架构
```
com.anthropic.claude/
├── client/
│   ├── ClaudeCodeClient.java      # 主客户端类
│   ├── AsyncQueryClient.java      # 异步查询客户端
│   └── SyncQueryClient.java       # 同步查询客户端
├── query/
│   ├── QueryService.java          # 查询服务接口
│   ├── QueryBuilder.java          # 查询构建器
│   └── StreamingResponse.java     # 流式响应处理
├── tools/
│   ├── ToolRegistry.java          # 工具注册中心
│   ├── ToolExecutor.java          # 工具执行器
│   └── MCPServer.java             # MCP服务器实现
├── hooks/
│   ├── HookManager.java           # Hook管理器
│   ├── HookRegistry.java          # Hook注册中心
│   └── EventDispatcher.java       # 事件分发器
├── subagents/
│   ├── SubagentManager.java       # 子代理管理器
│   ├── TaskDistributor.java       # 任务分发器
│   └── ResultAggregator.java      # 结果聚合器
├── process/
│   ├── ProcessManager.java        # 进程管理器
│   ├── AsyncProcessExecutor.java  # 异步进程执行器
│   └── StreamHandler.java         # 流处理器
├── config/
│   ├── Configuration.java         # 配置类
│   ├── ConfigLoader.java          # 配置加载器
│   └── EnvironmentConfig.java     # 环境配置
├── models/
│   ├── Message.java              # 消息模型
│   ├── Tool.java                 # 工具模型
│   ├── Hook.java                 # Hook模型
│   └── Response.java             # 响应模型
└── utils/
    ├── JsonUtils.java            # JSON工具类
    ├── StreamUtils.java          # 流处理工具
    └── ValidationUtils.java      # 验证工具类
```

### 第二步：核心API设计

#### 1. 主客户端API
```java
public class ClaudeCodeClient {
    // 构造函数
    public ClaudeCodeClient(Configuration config);

    // 同步查询
    public Response query(String prompt);

    // 异步查询
    public CompletableFuture<Response> queryAsync(String prompt);

    // 流式查询
    public Observable<Message> queryStream(String prompt);

    // 工具注册
    public void registerTool(Tool tool);

    // Hook注册
    public void registerHook(Hook hook);
}
```

#### 2. 查询构建器API
```java
public class QueryBuilder {
    public QueryBuilder withPrompt(String prompt);
    public QueryBuilder withModel(String model);
    public QueryBuilder withTimeout(Duration timeout);
    public QueryBuilder withMaxTokens(int maxTokens);
    public QueryBuilder withTools(List<Tool> tools);
    public Query build();
}
```

#### 3. 工具注册API
```java
@Tool(name = "calculator", description = "Performs calculations")
public class CalculatorTool {
    @ToolMethod
    public double add(double a, double b) {
        return a + b;
    }
}
```

### 第三步：技术栈映射

| Python技术 | Java对应技术 |
|-----------|------------|
| asyncio | CompletableFuture + RxJava |
| subprocess | ProcessBuilder |
| json | Jackson |
| typing | Java泛型 + 注解 |
| dataclasses | Record类 (Java 14+) 或 POJO |
| abc | Java接口/抽象类 |
| contextlib | try-with-resources |
| threading | java.util.concurrent |

### 第四步：实现优先级

1. **高优先级**（第一周）
   - ClaudeCodeClient 主客户端
   - QueryService 查询服务
   - Configuration 配置管理
   - Message/Response 数据模型

2. **中优先级**（第二周）
   - ToolRegistry 工具系统
   - HookManager Hook系统
   - ProcessManager 进程管理

3. **低优先级**（第三周）
   - SubagentManager 子代理系统
   - MCPServer MCP服务器
   - 高级功能和优化

## 下一步行动

1. 创建Java项目基础结构
2. 实现核心客户端类
3. 开发查询服务
4. 添加工具和Hook支持
5. 编写单元测试
6. 创建使用示例
7. 编写文档