# Claude Agent SDK Java - v2.0.0 升级规划

> **创建日期**: 2025-10-21
> **目标版本**: v2.0.0 (Claude Agent SDK)
> **当前版本**: v1.0.0 (Claude Code SDK)
> **Python SDK 对标**: v0.1.4

---

## 📋 执行摘要

本次升级将 **Claude Code Java SDK** 现代化为 **Claude Agent SDK**，与官方 Python SDK v0.1.4 保持功能对等。

### 关键目标
1. ✅ **重命名**: ClaudeCode → ClaudeAgent（对齐官方命名）
2. ✅ **新功能**: 添加 5 个 Python SDK v0.1.x 新特性
3. ✅ **向后兼容**: 通过 @Deprecated 过渡，避免突然 Breaking
4. ✅ **企业就绪**: 完整测试覆盖、文档和迁移指南

### 影响范围
- **文件数量**: 82 个 Java 文件需要重命名/修改
- **包结构**: `com.anthropic.claude` → 保持不变（内部实现）
- **公共 API**: 13 个主要类需要重命名
- **依赖项**: 无外部依赖变更

### 时间估算
- **Phase 1 (准备阶段)**: 2-3 天
- **Phase 2 (核心重构)**: 5-7 天
- **Phase 3 (新功能)**: 10-14 天
- **Phase 4 (测试&文档)**: 3-5 天
- **总计**: 20-29 天 (4-6 周)

---

## 🎯 升级路线图

### Phase 1: 准备和分析 (Week 1)

#### 1.1 代码分析和依赖图
**任务**:
- [ ] 生成完整的类依赖关系图
- [ ] 识别所有需要重命名的类、接口、枚举
- [ ] 扫描外部使用场景（GUI 模块、示例代码）

**产出**:
- `dependency-graph.md` - 类依赖关系图
- `rename-checklist.md` - 重命名清单（包含影响分析）

#### 1.2 创建兼容层设计
**任务**:
- [ ] 设计 @Deprecated 兼容类（ClaudeCodeSDK → ClaudeAgentSDK）
- [ ] 规划包别名和迁移路径
- [ ] 设计版本检测和警告机制

**产出**:
- `compatibility-layer-design.md`

#### 1.3 分支策略
**任务**:
- [ ] 创建 `feature/v2.0.0-upgrade` 分支
- [ ] 设置 GitHub Projects 跟踪进度
- [ ] 配置 CI/CD 流水线支持新分支

**产出**:
- 工作分支和项目看板

---

### Phase 2: 核心重构 - 重命名 (Week 2-3)

#### 2.1 创建新的核心类

**重命名清单**:

| 旧类名 | 新类名 | 包路径 | 优先级 |
|--------|--------|---------|--------|
| `ClaudeCodeSDK` | `ClaudeAgentSDK` | `com.anthropic.claude.client` | 🔴 高 |
| `ClaudeCodeOptions` | `ClaudeAgentOptions` | `com.anthropic.claude.config` | 🔴 高 |
| `ClaudeCodeException` | `ClaudeAgentException` | `com.anthropic.claude.exceptions` | 🔴 高 |
| `ClaudeSDKClient` | 保持不变 | `com.anthropic.claude.client` | - |
| `SessionManager` | 保持不变 | `com.anthropic.claude.client` | - |

**实施步骤**:

1. **创建新类** (保留旧类)
   ```bash
   # 示例：创建 ClaudeAgentSDK
   cp ClaudeCodeSDK.java ClaudeAgentSDK.java
   # 修改类名和内部引用
   ```

2. **添加 @Deprecated 到旧类**
   ```java
   /**
    * @deprecated 自 v2.0.0 起已弃用，请使用 {@link ClaudeAgentSDK}
    * 将在 v3.0.0 中移除
    */
   @Deprecated(since = "2.0.0", forRemoval = true)
   public class ClaudeCodeSDK {
       private final ClaudeAgentSDK delegate;

       public ClaudeCodeSDK() {
           this.delegate = new ClaudeAgentSDK();
           logDeprecationWarning();
       }

       // 委托所有方法到新类
       public CompletableFuture<Stream<Message>> query(String prompt) {
           return delegate.query(prompt);
       }
   }
   ```

3. **更新内部引用**
   - 逐步将内部代码迁移到新类
   - 保持旧类作为简单包装器

#### 2.2 更新配置系统

**变更**:

```java
// 旧: ClaudeCodeOptions
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .apiKey("...")
    .build();

// 新: ClaudeAgentOptions (v2.0.0)
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey("...")
    .settingSources(List.of("env", "user_config"))  // 新增
    .build();

// 兼容: ClaudeCodeOptions 委托到 ClaudeAgentOptions
@Deprecated
public class ClaudeCodeOptions {
    public static ClaudeAgentOptions.Builder builder() {
        return ClaudeAgentOptions.builder();
    }
}
```

#### 2.3 更新异常体系

```java
// 新: ClaudeAgentException
public class ClaudeAgentException extends RuntimeException {
    // 所有现有功能
}

// 旧: ClaudeCodeException (委托)
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeException extends ClaudeAgentException {
    public ClaudeCodeException(String message) {
        super(message);
    }
}
```

---

### Phase 3: 新功能实现 (Week 3-5)

#### 3.1 Session Forking 🔴 高优先级

**目标**: 允许从会话的任意时刻创建分支，探索不同解决方案。

**API 设计**:

```java
public class SessionManager {

    /**
     * 从现有会话创建分支
     *
     * @param parentSessionId 父会话 ID
     * @return 新分支的会话 ID
     */
    public String forkSession(String parentSessionId) {
        Session parent = getSession(parentSessionId);
        if (parent == null) {
            throw new ClaudeAgentException("Parent session not found: " + parentSessionId);
        }

        // 创建新会话，继承父会话上下文
        String forkId = generateSessionId();
        Session fork = new Session(forkId, parent);
        sessions.put(forkId, fork);

        logger.info("创建会话分支: {} -> {}", parentSessionId, forkId);
        return forkId;
    }

    /**
     * 获取会话的所有分支
     */
    public List<String> getSessionForks(String sessionId) {
        return sessions.values().stream()
            .filter(s -> sessionId.equals(s.getParentSessionId()))
            .map(Session::getSessionId)
            .collect(Collectors.toList());
    }
}
```

**Session 类增强**:

```java
public static class Session {
    private final String sessionId;
    private final String parentSessionId;  // 新增：父会话 ID
    private final LocalDateTime createdAt;
    private final LocalDateTime forkedAt;  // 新增：分支创建时间
    private final List<Message> inheritedMessages;  // 新增：继承的消息

    // 新增：分支构造函数
    public Session(String sessionId, Session parent) {
        this.sessionId = sessionId;
        this.parentSessionId = parent.getSessionId();
        this.createdAt = LocalDateTime.now();
        this.forkedAt = LocalDateTime.now();
        this.lastActivity = System.currentTimeMillis();
        this.active = true;

        // 深拷贝父会话的消息历史
        this.inheritedMessages = new ArrayList<>(parent.getMessages());
    }

    public String getParentSessionId() { return parentSessionId; }
    public boolean isFork() { return parentSessionId != null; }
}
```

**CLI 集成**:

```java
// QueryService 支持会话分支
public CompletableFuture<Stream<Message>> queryWithFork(QueryRequest request, String forkFromSessionId) {
    if (forkFromSessionId != null) {
        // 使用 --continue <session-id> 参数继承上下文
        // 但创建新的会话 ID 进行隔离
        request = request.toBuilder()
            .continueSession(forkFromSessionId)
            .build();
    }
    return queryAsync(request);
}
```

**测试用例**:

```java
@Test
void testSessionForking() {
    SessionManager manager = new SessionManager(options);

    // 创建父会话
    String parentId = manager.createSession();
    manager.incrementMessageCount(parentId);

    // 创建分支
    String forkId = manager.forkSession(parentId);

    // 验证分支独立性
    assertNotEquals(parentId, forkId);
    assertTrue(manager.isSessionActive(forkId));

    Session fork = manager.getSession(forkId);
    assertEquals(parentId, fork.getParentSessionId());
    assertTrue(fork.isFork());
}
```

---

#### 3.2 Programmatic Subagents 🔴 高优先级

**目标**: 在代码中直接定义子代理，无需文件系统依赖。

**API 设计**:

```java
// 新类: SubagentDefinition
package com.anthropic.claude.subagents;

public class SubagentDefinition {
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final List<String> tools;
    private final String model;
    private final Map<String, Object> metadata;

    private SubagentDefinition(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.systemPrompt = builder.systemPrompt;
        this.tools = builder.tools;
        this.model = builder.model;
        this.metadata = builder.metadata;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String description;
        private String systemPrompt;
        private List<String> tools = new ArrayList<>();
        private String model = "claude-sonnet-4-5";
        private Map<String, Object> metadata = new HashMap<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder tools(String... tools) {
            this.tools.addAll(Arrays.asList(tools));
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public SubagentDefinition build() {
            if (description == null || systemPrompt == null) {
                throw new IllegalStateException("description and systemPrompt are required");
            }
            return new SubagentDefinition(this);
        }
    }
}
```

**SubagentManager 增强**:

```java
public class SubagentManager {
    private final Map<String, SubagentDefinition> inlineAgents = new ConcurrentHashMap<>();

    /**
     * 注册内联子代理定义
     */
    public void registerInlineAgent(SubagentDefinition definition) {
        inlineAgents.put(definition.getName(), definition);
        logger.info("注册内联子代理: {}", definition.getName());
    }

    /**
     * 启动内联定义的子代理
     */
    public String startInlineSubagent(String agentName, Map<String, Object> context) {
        SubagentDefinition def = inlineAgents.get(agentName);
        if (def == null) {
            throw new ClaudeAgentException("Inline agent not found: " + agentName);
        }

        // 将定义转换为 CLI 参数
        List<String> args = new ArrayList<>();
        args.add("--agent-name");
        args.add(def.getName());
        args.add("--agent-prompt");
        args.add(def.getSystemPrompt());
        args.add("--agent-tools");
        args.add(String.join(",", def.getTools()));
        args.add("--model");
        args.add(def.getModel());

        // 启动子代理进程
        return startSubagentWithArgs(args, context);
    }

    /**
     * 批量注册子代理
     */
    public void registerInlineAgents(Map<String, SubagentDefinition> agents) {
        agents.forEach((name, def) -> registerInlineAgent(def));
    }
}
```

**ClaudeAgentOptions 集成**:

```java
public class ClaudeAgentOptions {
    private final Map<String, SubagentDefinition> agents;  // 新增

    public static class Builder {
        private Map<String, SubagentDefinition> agents = new HashMap<>();

        /**
         * 添加内联子代理定义
         */
        public Builder addAgent(SubagentDefinition agent) {
            this.agents.put(agent.getName(), agent);
            return this;
        }

        /**
         * 批量添加子代理
         */
        public Builder agents(Map<String, SubagentDefinition> agents) {
            this.agents.putAll(agents);
            return this;
        }
    }

    public Map<String, SubagentDefinition> getAgents() {
        return new HashMap<>(agents);
    }
}
```

**使用示例**:

```java
// 定义子代理
SubagentDefinition codeAnalyzer = SubagentDefinition.builder("code-analyzer")
    .description("Analyzes code quality and suggests improvements")
    .systemPrompt("You are an expert code reviewer specializing in Java")
    .tools("Read", "Grep", "Glob")
    .model("claude-sonnet-4-5")
    .build();

SubagentDefinition testWriter = SubagentDefinition.builder("test-writer")
    .description("Writes comprehensive unit tests")
    .systemPrompt("You write excellent JUnit 5 tests with high coverage")
    .tools("Read", "Write", "Edit")
    .build();

// 创建 SDK 实例
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .addAgent(codeAnalyzer)
    .addAgent(testWriter)
    .build();

ClaudeAgentSDK sdk = new ClaudeAgentSDK(options);

// 启动子代理
String agentId = sdk.getSubagentManager().startInlineSubagent("code-analyzer", context);
```

---

#### 3.3 Settings Sources Configuration 🟡 中优先级

**目标**: 精细控制配置加载源，支持自定义优先级。

**API 设计**:

```java
// 新枚举: SettingSource
package com.anthropic.claude.config;

public enum SettingSource {
    DEFAULTS("defaults", 0),
    ENV_VARS("env", 1),
    USER_CONFIG("user_config", 2),
    PROJECT_CONFIG("project_config", 3),
    CLAUDE_MD("claude_md", 4),
    SLASH_COMMANDS("slash_commands", 5),
    INLINE_OPTIONS("inline", 10);  // 最高优先级

    private final String name;
    private final int priority;

    SettingSource(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public String getName() { return name; }
    public int getPriority() { return priority; }
}
```

**ClaudeAgentOptions 增强**:

```java
public class ClaudeAgentOptions {
    private final List<SettingSource> settingSources;  // 新增

    public static class Builder {
        // v2.0.0 默认值变更：不再自动加载所有源
        private List<SettingSource> settingSources = Arrays.asList(
            SettingSource.ENV_VARS,
            SettingSource.USER_CONFIG,
            SettingSource.INLINE_OPTIONS
        );

        /**
         * 自定义配置源（覆盖默认）
         */
        public Builder settingSources(SettingSource... sources) {
            this.settingSources = Arrays.asList(sources);
            return this;
        }

        /**
         * 添加配置源（追加）
         */
        public Builder addSettingSource(SettingSource source) {
            if (!this.settingSources.contains(source)) {
                List<SettingSource> newSources = new ArrayList<>(this.settingSources);
                newSources.add(source);
                this.settingSources = newSources;
            }
            return this;
        }

        /**
         * 使用 v1.x 兼容模式（加载所有源）
         */
        public Builder useV1CompatibilityMode() {
            this.settingSources = Arrays.asList(SettingSource.values());
            return this;
        }
    }
}
```

**ConfigLoader 重构**:

```java
public class ConfigLoader {

    /**
     * 根据 settingSources 加载配置
     */
    public ClaudeAgentOptions loadConfiguration(List<SettingSource> sources) {
        Map<String, Object> mergedConfig = new HashMap<>();

        // 按优先级排序（低到高）
        List<SettingSource> sortedSources = sources.stream()
            .sorted(Comparator.comparingInt(SettingSource::getPriority))
            .collect(Collectors.toList());

        // 依次加载并合并
        for (SettingSource source : sortedSources) {
            Map<String, Object> config = loadFromSource(source);
            mergedConfig.putAll(config);  // 高优先级覆盖低优先级
            logger.debug("已加载配置源: {} (优先级: {})", source.getName(), source.getPriority());
        }

        return buildOptionsFromConfig(mergedConfig);
    }

    private Map<String, Object> loadFromSource(SettingSource source) {
        switch (source) {
            case DEFAULTS:
                return loadDefaults();
            case ENV_VARS:
                return loadEnvironmentVariables();
            case USER_CONFIG:
                return loadUserConfig();
            case CLAUDE_MD:
                return loadClaudeMd();
            case SLASH_COMMANDS:
                return loadSlashCommands();
            case INLINE_OPTIONS:
                return Collections.emptyMap();  // 由 Builder 直接设置
            default:
                return Collections.emptyMap();
        }
    }
}
```

**Breaking Change 警告**:

```java
// ClaudeAgentSDK 初始化时检查
public ClaudeAgentSDK(ClaudeAgentOptions options) {
    if (options.getSettingSources() == null || options.getSettingSources().isEmpty()) {
        logger.warn("⚠️ v2.0.0 Breaking Change: settingSources 未指定，使用新默认值 (env, user_config, inline)");
        logger.warn("如需 v1.x 行为，请调用 .useV1CompatibilityMode()");
    }
    // ...
}
```

---

#### 3.4 Custom Transport 抽象层 🟡 中优先级

**目标**: 解耦 CLI 进程执行，支持自定义通信层。

**API 设计**:

```java
// 新接口: ClaudeTransport
package com.anthropic.claude.transport;

public interface ClaudeTransport extends AutoCloseable {

    /**
     * 发送消息到 Claude
     */
    void send(String message) throws TransportException;

    /**
     * 接收 Claude 响应流
     */
    Observable<String> receive() throws TransportException;

    /**
     * 检查连接状态
     */
    boolean isConnected();

    /**
     * 关闭传输层
     */
    @Override
    void close() throws TransportException;

    /**
     * 获取传输层类型
     */
    TransportType getType();
}

// 传输层类型
public enum TransportType {
    CLI_PROCESS,    // 现有实现
    HTTP_API,       // 未来扩展
    WEBSOCKET,      // 未来扩展
    CUSTOM          // 用户自定义
}
```

**默认实现: ProcessTransport**:

```java
package com.anthropic.claude.transport;

public class ProcessTransport implements ClaudeTransport {
    private final ProcessManager processManager;
    private final ClaudeAgentOptions options;
    private Process cliProcess;

    public ProcessTransport(ProcessManager processManager, ClaudeAgentOptions options) {
        this.processManager = processManager;
        this.options = options;
    }

    @Override
    public void send(String message) throws TransportException {
        try {
            if (cliProcess == null) {
                startCliProcess();
            }
            OutputStream stdin = cliProcess.getOutputStream();
            stdin.write(message.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (IOException e) {
            throw new TransportException("Failed to send message", e);
        }
    }

    @Override
    public Observable<String> receive() throws TransportException {
        if (cliProcess == null) {
            throw new TransportException("Process not started");
        }

        return Observable.create(emitter -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(cliProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    emitter.onNext(line);
                }
                emitter.onComplete();
            } catch (IOException e) {
                emitter.onError(new TransportException("Failed to receive", e));
            }
        });
    }

    @Override
    public boolean isConnected() {
        return cliProcess != null && cliProcess.isAlive();
    }

    @Override
    public void close() throws TransportException {
        if (cliProcess != null) {
            cliProcess.destroy();
        }
    }

    @Override
    public TransportType getType() {
        return TransportType.CLI_PROCESS;
    }

    private void startCliProcess() throws TransportException {
        try {
            this.cliProcess = processManager.startProcess(
                options.getCliPath(),
                buildCliArgs()
            );
        } catch (Exception e) {
            throw new TransportException("Failed to start CLI process", e);
        }
    }
}
```

**ClaudeAgentOptions 集成**:

```java
public class ClaudeAgentOptions {
    private final ClaudeTransport customTransport;  // 新增

    public static class Builder {
        private ClaudeTransport customTransport;

        /**
         * 使用自定义传输层
         */
        public Builder transport(ClaudeTransport transport) {
            this.customTransport = transport;
            return this;
        }
    }

    public ClaudeTransport getCustomTransport() {
        return customTransport;
    }
}
```

**QueryService 重构**:

```java
public class QueryService {
    private final ClaudeTransport transport;

    public QueryService(ClaudeTransport transport, HookService hookService, ClaudeAgentOptions options) {
        this.transport = transport;
        this.hookService = hookService;
        this.options = options;
    }

    // 兼容构造函数
    public QueryService(ProcessManager processManager, HookService hookService, ClaudeAgentOptions options) {
        this(new ProcessTransport(processManager, options), hookService, options);
    }

    public Observable<Message> queryStream(QueryRequest request) {
        return transport.receive()
            .map(line -> messageParser.parse(line))
            .filter(Objects::nonNull);
    }
}
```

**自定义传输层示例**:

```java
// 用户可以实现自定义传输层
public class HttpApiTransport implements ClaudeTransport {
    private final OkHttpClient httpClient;

    @Override
    public void send(String message) {
        // 通过 HTTP POST 发送
        Request request = new Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(RequestBody.create(message, MediaType.get("application/json")))
            .build();
        httpClient.newCall(request).execute();
    }

    @Override
    public Observable<String> receive() {
        // 通过 SSE 或 WebSocket 接收
        // ...
    }
}

// 使用自定义传输层
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .transport(new HttpApiTransport())
    .build();
```

---

#### 3.5 Enhanced Hook Fields 🟢 低优先级

**目标**: 添加缺失的 Hook 输出字段，对齐 Python SDK。

**HookResult 增强**:

```java
package com.anthropic.claude.hooks;

public class HookResult {
    private final HookDecision decision;        // 新增：APPROVE/BLOCK
    private final String reason;                // 重命名自 message
    private final boolean continue_;            // 重命名自 shouldContinue
    private final boolean suppressOutput;       // 新增
    private final String stopReason;            // 新增
    private final Map<String, Object> modifiedData;

    public enum HookDecision {
        APPROVE,
        BLOCK
    }

    private HookResult(Builder builder) {
        this.decision = builder.decision;
        this.reason = builder.reason;
        this.continue_ = builder.continue_;
        this.suppressOutput = builder.suppressOutput;
        this.stopReason = builder.stopReason;
        this.modifiedData = builder.modifiedData;
    }

    public static Builder builder() {
        return new Builder();
    }

    // 快捷方法
    public static HookResult approve() {
        return builder().decision(HookDecision.APPROVE).build();
    }

    public static HookResult block(String reason) {
        return builder()
            .decision(HookDecision.BLOCK)
            .reason(reason)
            .continue_(false)
            .build();
    }

    public static HookResult suppress() {
        return builder()
            .decision(HookDecision.APPROVE)
            .suppressOutput(true)
            .build();
    }

    public static class Builder {
        private HookDecision decision = HookDecision.APPROVE;
        private String reason;
        private boolean continue_ = true;
        private boolean suppressOutput = false;
        private String stopReason;
        private Map<String, Object> modifiedData = new HashMap<>();

        public Builder decision(HookDecision decision) {
            this.decision = decision;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder continue_(boolean continue_) {
            this.continue_ = continue_;
            return this;
        }

        public Builder suppressOutput(boolean suppressOutput) {
            this.suppressOutput = suppressOutput;
            return this;
        }

        public Builder stopReason(String stopReason) {
            this.stopReason = stopReason;
            this.continue_ = false;  // 自动设置
            return this;
        }

        public Builder modifiedData(Map<String, Object> data) {
            this.modifiedData = new HashMap<>(data);
            return this;
        }

        public HookResult build() {
            return new HookResult(this);
        }
    }

    // Getters
    public HookDecision getDecision() { return decision; }
    public String getReason() { return reason; }
    public boolean shouldContinue() { return continue_; }
    public boolean shouldSuppressOutput() { return suppressOutput; }
    public String getStopReason() { return stopReason; }
    public Map<String, Object> getModifiedData() { return modifiedData; }

    // 兼容性方法
    @Deprecated(since = "2.0.0")
    public String getMessage() { return reason; }
}
```

**HookService 更新**:

```java
public class HookService {

    public HookResult executeHook(String eventType, HookContext context) {
        List<HookCallback> callbacks = hooks.get(eventType);
        if (callbacks == null || callbacks.isEmpty()) {
            return HookResult.approve();
        }

        HookResult finalResult = HookResult.approve();

        for (HookCallback callback : callbacks) {
            try {
                HookResult result = callback.execute(context);

                // 处理 BLOCK 决策
                if (result.getDecision() == HookResult.HookDecision.BLOCK) {
                    logger.warn("Hook {} 阻止了操作: {}", eventType, result.getReason());
                    return result;  // 立即返回
                }

                // 处理 suppressOutput
                if (result.shouldSuppressOutput()) {
                    logger.debug("Hook {} 抑制输出", eventType);
                    finalResult = result;
                }

                // 处理 stopReason
                if (result.getStopReason() != null) {
                    logger.info("Hook {} 停止执行: {}", eventType, result.getStopReason());
                    return result;
                }

                // 合并修改的数据
                if (!result.getModifiedData().isEmpty()) {
                    context.mergeData(result.getModifiedData());
                }

            } catch (Exception e) {
                logger.error("Hook {} 执行失败", eventType, e);
            }
        }

        return finalResult;
    }
}
```

**使用示例**:

```java
// 阻止特定操作
sdk.addHook("pre_query", context -> {
    String prompt = (String) context.getData().get("prompt");
    if (prompt.contains("dangerous")) {
        return HookResult.block("包含危险关键词");
    }
    return HookResult.approve();
});

// 抑制输出
sdk.addHook("post_query", context -> {
    if (context.getData().get("internal_query") == Boolean.TRUE) {
        return HookResult.suppress();
    }
    return HookResult.approve();
});

// 提前停止
sdk.addHook("query_progress", context -> {
    if (shouldCancel) {
        return HookResult.builder()
            .stopReason("用户取消")
            .build();
    }
    return HookResult.approve();
});
```

---

### Phase 4: 测试和文档 (Week 6)

#### 4.1 单元测试

**新增测试类**:

1. **SessionForkingTest.java**
   ```java
   @Test
   void testForkSessionInheritsContext()
   @Test
   void testForkSessionIndependence()
   @Test
   void testMultipleForks()
   @Test
   void testForkNonexistentSession()
   ```

2. **ProgrammaticSubagentsTest.java**
   ```java
   @Test
   void testRegisterInlineAgent()
   @Test
   void testStartInlineSubagent()
   @Test
   void testInlineAgentOverridesFilesystem()
   ```

3. **SettingSourcesTest.java**
   ```java
   @Test
   void testCustomSettingSources()
   @Test
   void testSettingSourcePriority()
   @Test
   void testV1CompatibilityMode()
   ```

4. **CustomTransportTest.java**
   ```java
   @Test
   void testProcessTransport()
   @Test
   void testCustomTransportIntegration()
   ```

5. **EnhancedHookFieldsTest.java**
   ```java
   @Test
   void testHookDecision()
   @Test
   void testSuppressOutput()
   @Test
   void testStopReason()
   ```

**目标覆盖率**: >85%

#### 4.2 集成测试

```java
@Test
void testEndToEndWithAllNewFeatures() {
    // 1. 使用新 API
    SubagentDefinition agent = SubagentDefinition.builder("test-agent")
        .description("Test agent")
        .systemPrompt("You are a test agent")
        .tools("Read")
        .build();

    ClaudeAgentOptions options = ClaudeAgentOptions.builder()
        .apiKey(TEST_API_KEY)
        .addAgent(agent)
        .settingSources(SettingSource.ENV_VARS, SettingSource.INLINE_OPTIONS)
        .build();

    ClaudeAgentSDK sdk = new ClaudeAgentSDK(options);

    // 2. 测试会话分支
    String sessionId = sdk.getSessionManager().createSession();
    String fork1 = sdk.getSessionManager().forkSession(sessionId);
    String fork2 = sdk.getSessionManager().forkSession(sessionId);

    assertNotEquals(fork1, fork2);

    // 3. 测试内联子代理
    String agentId = sdk.getSubagentManager()
        .startInlineSubagent("test-agent", Map.of());
    assertNotNull(agentId);

    // 4. 测试增强 Hook
    AtomicBoolean hookExecuted = new AtomicBoolean(false);
    sdk.addHook("pre_query", ctx -> {
        hookExecuted.set(true);
        return HookResult.approve();
    });

    sdk.query("test").join();
    assertTrue(hookExecuted.get());
}
```

#### 4.3 文档更新

**需要创建/更新的文档**:

1. **MIGRATION_GUIDE_V2.md** ⭐ 重要
   - 重命名映射表
   - Breaking Changes 详细说明
   - 代码迁移示例（Before/After）
   - 常见问题 FAQ

2. **UPGRADE_PLAN_V2.md** (本文档)
   - 升级规划总览

3. **CHANGELOG.md**
   - v2.0.0 所有变更记录
   - 新功能详细描述
   - Breaking Changes 警告

4. **README.md**
   - 更新项目名称：Claude Agent SDK Java
   - 更新快速开始示例
   - 添加 v2.0.0 新特性展示

5. **API_REFERENCE_V2.md**
   - 完整的 v2.0.0 API 文档
   - 新类、方法的详细说明
   - 使用示例

6. **CLAUDE.md**
   - 更新 Claude Code 工作指南
   - 反映新的项目结构

---

## 🔄 Breaking Changes 管理

### 策略

1. **向后兼容层** (v2.0.0 - v2.9.x)
   - 保留所有旧类，标记 @Deprecated
   - 旧类委托到新类实现
   - 运行时警告日志

2. **迁移期** (6 个月)
   - 提供详细迁移指南
   - 工具辅助代码迁移（可选）
   - 社区支持和答疑

3. **彻底移除** (v3.0.0)
   - 删除所有 @Deprecated 类
   - 仅保留新 API

### Breaking Changes 清单

| 变更 | 影响 | 迁移路径 | 移除版本 |
|------|------|----------|----------|
| ClaudeCodeSDK → ClaudeAgentSDK | 🔴 高 | 使用新类名 | v3.0.0 |
| ClaudeCodeOptions → ClaudeAgentOptions | 🔴 高 | 使用新类名 | v3.0.0 |
| ClaudeCodeException → ClaudeAgentException | 🟡 中 | 更新异常捕获 | v3.0.0 |
| settingSources 默认值变更 | 🟡 中 | 调用 .useV1CompatibilityMode() | N/A |
| HookResult.message → reason | 🟢 低 | 使用 getReason() | v3.0.0 |
| HookResult.shouldContinue → continue_ | 🟢 低 | 使用 shouldContinue() | v3.0.0 |

### 弃用警告示例

```java
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeSDK {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeSDK.class);
    private static final AtomicBoolean WARNING_LOGGED = new AtomicBoolean(false);

    public ClaudeCodeSDK() {
        logDeprecationWarning();
        // ...
    }

    private void logDeprecationWarning() {
        if (WARNING_LOGGED.compareAndSet(false, true)) {
            logger.warn("⚠️  ClaudeCodeSDK 已弃用，请使用 ClaudeAgentSDK");
            logger.warn("⚠️  ClaudeCodeSDK 将在 v3.0.0 中移除");
            logger.warn("⚠️  迁移指南: https://github.com/.../MIGRATION_GUIDE_V2.md");
        }
    }
}
```

---

## 📊 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 大规模重命名导致遗漏 | 🟡 中 | 自动化工具扫描、完整测试覆盖 |
| Breaking Changes 影响用户 | 🟡 中 | 向后兼容层、详细迁移指南 |
| 新功能 Bug | 🟡 中 | 全面单元测试、集成测试、Beta 测试期 |
| 文档不同步 | 🟢 低 | 文档与代码同步提交、Review 检查 |
| 性能退化 | 🟢 低 | 基准测试、性能回归测试 |

---

## ✅ 验收标准

### 功能完整性
- [ ] 5 个新功能全部实现并通过测试
- [ ] 所有旧 API 都有对应的新 API
- [ ] 向后兼容层正常工作

### 质量标准
- [ ] 单元测试覆盖率 >85%
- [ ] 所有集成测试通过
- [ ] 无严重性能退化（<5% 性能损失）
- [ ] 无内存泄漏

### 文档完整性
- [ ] MIGRATION_GUIDE_V2.md 完成
- [ ] API_REFERENCE_V2.md 完成
- [ ] CHANGELOG.md 更新
- [ ] README.md 更新
- [ ] 所有公共 API 有 JavaDoc

### 发布准备
- [ ] v2.0.0-beta.1 发布并测试
- [ ] 社区反馈收集并修复
- [ ] v2.0.0-rc.1 发布
- [ ] 最终 v2.0.0 发布

---

## 📅 里程碑

| 里程碑 | 目标日期 | 交付物 |
|--------|----------|--------|
| M1: 准备完成 | Week 1 结束 | 依赖图、设计文档、分支策略 |
| M2: 核心重构完成 | Week 3 结束 | 所有类重命名、向后兼容层 |
| M3: 新功能 50% | Week 4 结束 | Session Forking、Programmatic Subagents |
| M4: 新功能 100% | Week 5 结束 | 所有 5 个新功能实现 |
| M5: 测试完成 | Week 6 结束 | 所有测试通过、文档完成 |
| M6: Beta 发布 | Week 7 | v2.0.0-beta.1 |
| M7: 正式发布 | Week 8 | v2.0.0 |

---

## 🚀 下一步行动

### 立即执行（本周）
1. [ ] 创建 `feature/v2.0.0-upgrade` 分支
2. [ ] 创建 GitHub Project 跟踪任务
3. [ ] 生成类依赖关系图
4. [ ] 开始第一个重命名：ClaudeCodeSDK → ClaudeAgentSDK

### 本月内
1. [ ] 完成核心类重命名（Phase 2）
2. [ ] 实现 Session Forking（Phase 3.1）
3. [ ] 实现 Programmatic Subagents（Phase 3.2）

### 长期
1. [ ] 完成所有新功能
2. [ ] Beta 测试和反馈收集
3. [ ] 正式发布 v2.0.0

---

## 📞 联系和支持

- **项目负责人**: [待定]
- **技术审核**: [待定]
- **问题跟踪**: GitHub Issues
- **进度看板**: GitHub Projects

---

**版本**: v1.0
**最后更新**: 2025-10-21
**状态**: 规划中
