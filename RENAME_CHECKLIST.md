# v2.0.0 重命名清单

> **创建日期**: 2025-10-21
> **基于**: dependency-graph.md 分析结果
> **目标**: ClaudeCode → ClaudeAgent 重命名，保持向后兼容

---

## 📊 重命名概览

### 关键发现

根据依赖分析：
- **总类数**: 66 个公共类
- **需重命名**: 3 个核心类
- **保持不变**: 63 个类
- **外部消费者**: GUI 模块（10 处导入），测试套件

⚠️ **风险评估**:
- **GUI 模块高耦合**: 10 个直接 SDK 导入
- **重命名影响**: 如直接重命名会破坏 GUI 模块
- **缓解策略**: 使用委托模式 + @Deprecated

---

## 🎯 重命名策略

### 方案选择：**委托模式（推荐）**

✅ **优势**:
- 100% 向后兼容
- 平滑迁移路径
- 6 个月过渡期

❌ **劣势**:
- 临时代码冗余
- 需维护两套类（直到 v3.0.0）

### 替代方案对比

| 方案 | 向后兼容 | GUI 影响 | 迁移成本 | 推荐度 |
|------|----------|----------|----------|--------|
| **委托模式** | ✅ 100% | ✅ 无影响 | 🟢 低 | ⭐⭐⭐⭐⭐ |
| 直接重命名 | ❌ 破坏性 | ❌ 需同步修改 | 🔴 高 | ⭐ |
| 包别名 | ⚠️ 部分 | ⚠️ 需重新编译 | 🟡 中 | ⭐⭐ |

---

## 📋 重命名清单

### 1. 核心类重命名（3 个）

#### 1.1 ClaudeCodeSDK → ClaudeAgentSDK

**位置**: `com.anthropic.claude.client.ClaudeCodeSDK`

**影响范围**:
- **直接依赖**: 5 个（ConfigLoader, ProcessManager, QueryService, HookService, SubagentManager）
- **外部使用**: GUI 模块 2 处，示例代码 2 处，测试 3 处

**实施步骤**:
1. ✅ 创建新类 `ClaudeAgentSDK`（完整实现）
2. ✅ 保留 `ClaudeCodeSDK`（委托到 `ClaudeAgentSDK`）
3. ✅ 添加 `@Deprecated(since = "2.0.0", forRemoval = true)`
4. ✅ 添加运行时弃用警告日志
5. ✅ 更新内部引用使用新类

**代码示例**:
```java
// 新类：ClaudeAgentSDK.java
package com.anthropic.claude.client;

/**
 * Claude Agent SDK 主入口类
 * @since 2.0.0
 */
public class ClaudeAgentSDK {
    // 完整实现（从 ClaudeCodeSDK 复制并改进）
}

// 旧类：ClaudeCodeSDK.java（委托）
package com.anthropic.claude.client;

/**
 * @deprecated 自 v2.0.0 起已弃用，请使用 {@link ClaudeAgentSDK}
 *             将在 v3.0.0 中移除
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeSDK {
    private static final AtomicBoolean WARNING_LOGGED = new AtomicBoolean(false);
    private final ClaudeAgentSDK delegate;

    public ClaudeCodeSDK() {
        logDeprecationWarning();
        this.delegate = new ClaudeAgentSDK();
    }

    public ClaudeCodeSDK(ClaudeCodeOptions options) {
        logDeprecationWarning();
        this.delegate = new ClaudeAgentSDK(convertOptions(options));
    }

    // 委托所有方法到 ClaudeAgentSDK
    public CompletableFuture<Stream<Message>> query(String prompt) {
        return delegate.query(prompt);
    }

    // ... 其他方法类似委托
}
```

**测试验证**:
- [ ] 单元测试：新类所有功能
- [ ] 兼容性测试：旧类委托正确性
- [ ] 集成测试：GUI 模块无需修改仍可运行
- [ ] 警告测试：验证弃用警告输出

---

#### 1.2 ClaudeCodeOptions → ClaudeAgentOptions

**位置**: `com.anthropic.claude.config.ClaudeCodeOptions`

**影响范围**:
- **直接依赖**: ClaudeCodeSDK, ConfigLoader, QueryService, SubagentManager
- **外部使用**: GUI 模块 3 处，测试 5 处

**实施步骤**:
1. ✅ 创建 `ClaudeAgentOptions`（增强功能）
   - 新增 `settingSources` 字段
   - 新增 `agents` 字段（内联子代理）
   - 新增 `customTransport` 字段
2. ✅ 保留 `ClaudeCodeOptions`（委托到 Builder）
3. ✅ 添加 `@Deprecated` 注解
4. ✅ 提供 `.useV1CompatibilityMode()` 方法

**代码示例**:
```java
// 新类：ClaudeAgentOptions.java
package com.anthropic.claude.config;

public class ClaudeAgentOptions {
    // 所有旧字段 +
    private final List<SettingSource> settingSources;  // 新增
    private final Map<String, SubagentDefinition> agents;  // 新增
    private final ClaudeTransport customTransport;  // 新增

    public static class Builder {
        // v2.0.0 默认值变更
        private List<SettingSource> settingSources = Arrays.asList(
            SettingSource.ENV_VARS,
            SettingSource.USER_CONFIG,
            SettingSource.INLINE_OPTIONS
        );

        public Builder useV1CompatibilityMode() {
            this.settingSources = Arrays.asList(SettingSource.values());
            return this;
        }
    }
}

// 旧类：ClaudeCodeOptions.java（委托）
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeOptions {
    public static ClaudeAgentOptions.Builder builder() {
        return ClaudeAgentOptions.builder().useV1CompatibilityMode();
    }
}
```

**Breaking Change**:
⚠️ `settingSources` 默认值变更（不再自动加载 CLAUDE.md）
✅ 缓解：旧类自动调用 `.useV1CompatibilityMode()`

---

#### 1.3 ClaudeCodeException → ClaudeAgentException

**位置**: `com.anthropic.claude.exceptions.ClaudeCodeException`

**影响范围**:
- **抛出者**: 12 个类（ConfigLoader, ProcessManager, AuthFactory, QueryService 等）
- **捕获者**: GUI 模块 1 处，测试 8 处

**实施步骤**:
1. ✅ 创建 `ClaudeAgentException`（完整实现）
2. ✅ `ClaudeCodeException` 继承自 `ClaudeAgentException`
3. ✅ 添加 `@Deprecated` 注解
4. ✅ 内部代码逐步迁移到抛出新异常

**代码示例**:
```java
// 新类：ClaudeAgentException.java
package com.anthropic.claude.exceptions;

/**
 * Claude Agent SDK 基础异常类
 * @since 2.0.0
 */
public class ClaudeAgentException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;

    public ClaudeAgentException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.context = Collections.emptyMap();
    }

    public ClaudeAgentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    // ... 其他构造函数
}

// 旧类：ClaudeCodeException.java（继承）
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeException extends ClaudeAgentException {
    public ClaudeCodeException(String message) {
        super(message);
    }

    public ClaudeCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**兼容性保证**:
✅ 旧代码 `catch (ClaudeCodeException e)` 仍可捕获所有异常
✅ 新代码可以只捕获 `ClaudeAgentException`

---

### 2. 保持不变的类（63 个）

以下类**无需重命名**，保持原名：

#### 2.1 核心 API 类（保持）

| 类名 | 原因 | 状态 |
|------|------|------|
| `ClaudeSDKClient` | 已经是中性命名 | ✅ 保持 |
| `SessionManager` | 通用功能类 | ✅ 保持 |
| `Message` | 数据模型，广泛使用 | ✅ 保持 |
| `QueryRequest` | 请求模型 | ✅ 保持 |
| `QueryBuilder` | 构建器模式 | ✅ 保持 |
| `QueryService` | 服务类 | ✅ 保持 |

#### 2.2 配置和工具类（保持）

| 类名 | 原因 | 状态 |
|------|------|------|
| `ConfigLoader` | 配置加载器 | ✅ 保持 |
| `CliMode` | 枚举类型 | ✅ 保持 |
| `Configuration` | ⚠️ 考虑废弃（有 ClaudeAgentOptions） | 🟡 标记 @Deprecated |

#### 2.3 认证类（保持）

| 包 | 类数 | 状态 |
|----|----- |------|
| `com.anthropic.claude.auth` | 5 | ✅ 全部保持 |

#### 2.4 Hooks 系统（保持）

| 包 | 类数 | 状态 |
|----|----- |------|
| `com.anthropic.claude.hooks` | 4 | ✅ 全部保持 |

但 `HookResult` 需要**字段增强**（不是重命名）：
- 新增 `decision` 字段
- 新增 `suppressOutput` 字段
- 新增 `stopReason` 字段
- `message` → `reason`（添加兼容 getter）

#### 2.5 其他所有包（保持）

- `com.anthropic.claude.subagents` - 全部保持
- `com.anthropic.claude.process` - 全部保持
- `com.anthropic.claude.strategy` - 全部保持
- `com.anthropic.claude.pty` - 全部保持
- `com.anthropic.claude.tools` - 全部保持
- `com.anthropic.claude.context` - 全部保持
- `com.anthropic.claude.streaming` - 全部保持
- `com.anthropic.claude.performance` - 全部保持
- `com.anthropic.claude.utils` - 全部保持
- `com.anthropic.claude.models` - 全部保持
- `com.anthropic.claude.examples` - 全部保持

---

## 🗂️ 包结构变更

### 当前（v1.0.0）
```
com.anthropic.claude/
├── client/
│   ├── ClaudeCodeSDK        ⚠️ 重命名
│   ├── ClaudeSDKClient      ✅ 保持
│   └── SessionManager       ✅ 保持
├── config/
│   ├── ClaudeCodeOptions    ⚠️ 重命名
│   └── ...
└── exceptions/
    ├── ClaudeCodeException  ⚠️ 重命名
    └── ...
```

### 目标（v2.0.0）
```
com.anthropic.claude/
├── client/
│   ├── ClaudeAgentSDK       🆕 新增（主实现）
│   ├── ClaudeCodeSDK        ⚠️ @Deprecated（委托）
│   ├── ClaudeSDKClient      ✅ 保持
│   └── SessionManager       ✅ 保持（增强分支功能）
├── config/
│   ├── ClaudeAgentOptions   🆕 新增（主实现）
│   ├── ClaudeCodeOptions    ⚠️ @Deprecated（委托）
│   ├── SettingSource        🆕 新增（枚举）
│   └── ...
├── exceptions/
│   ├── ClaudeAgentException 🆕 新增（基类）
│   ├── ClaudeCodeException  ⚠️ @Deprecated（继承）
│   └── ...
├── transport/               🆕 新增包
│   ├── ClaudeTransport      🆕 接口
│   ├── ProcessTransport     🆕 CLI 实现
│   ├── TransportType        🆕 枚举
│   └── TransportException   🆕 异常
└── subagents/
    ├── SubagentManager      ✅ 保持（增强内联定义）
    ├── SubagentDefinition   🆕 新增
    └── ...
```

---

## 📈 外部影响分析

### GUI 模块影响

**当前依赖** (claude-code-gui):
```java
// 10 处导入
import com.anthropic.claude.client.ClaudeCodeSDK;         // 2 处
import com.anthropic.claude.config.ClaudeCodeOptions;     // 3 处
import com.anthropic.claude.exceptions.ClaudeCodeException; // 1 处
import com.anthropic.claude.messages.Message;             // 4 处
```

**v2.0.0 兼容性**:
✅ **无需修改**！委托模式确保完全兼容
⚠️ 运行时会看到弃用警告（可配置关闭）

**推荐迁移时间**:
- v2.0.0 发布后 3 个月内
- 在 v3.0.0 之前（预计 6-12 个月后）

### 测试套件影响

**测试文件分布**:
- `ClaudeCodeSDKTest.java` - 3 处使用
- `ClaudeCodeOptionsTest.java` - 5 处使用
- `ConfigLoaderTest.java` - 2 处使用

**迁移策略**:
1. **Phase 1**: 保留所有旧测试（验证兼容性）
2. **Phase 2**: 添加新类的测试
3. **Phase 3**: 逐步迁移旧测试到新 API
4. **v3.0.0**: 删除旧 API 测试

---

## ✅ 实施检查清单

### Phase 2.1: 创建新类（Week 2）

- [ ] 创建 `ClaudeAgentSDK.java`（复制自 ClaudeCodeSDK + 改进）
- [ ] 创建 `ClaudeAgentOptions.java`（复制 + 新增字段）
- [ ] 创建 `ClaudeAgentException.java`（新基类）
- [ ] 创建 `SettingSource.java`（新枚举）
- [ ] 创建 `transport` 包（4 个新类）
- [ ] 创建 `SubagentDefinition.java`（新类）

### Phase 2.2: 添加 @Deprecated（Week 2）

- [ ] `ClaudeCodeSDK` 添加委托实现
- [ ] `ClaudeCodeOptions` 添加委托 Builder
- [ ] `ClaudeCodeException` 改为继承新类
- [ ] `Configuration` 标记为 @Deprecated
- [ ] 所有弃用类添加运行时警告

### Phase 2.3: 更新内部引用（Week 3）

- [ ] `ConfigLoader` 使用 `ClaudeAgentOptions`
- [ ] `QueryService` 引用更新
- [ ] `SubagentManager` 引用更新
- [ ] 异常抛出迁移到新异常
- [ ] 示例代码更新（可选）

### Phase 2.4: 测试验证（Week 3）

- [ ] 单元测试：所有新类 >85% 覆盖
- [ ] 兼容性测试：旧 API 委托正确性
- [ ] 集成测试：GUI 模块运行无问题
- [ ] 性能测试：委托无显著性能损失 (<5%)
- [ ] 警告测试：弃用日志正确输出

---

## 📊 工作量估算

| 任务 | 文件数 | 预计工时 | 优先级 |
|------|--------|----------|--------|
| 创建新类 | 10 | 16 小时 | 🔴 高 |
| 添加委托/继承 | 3 | 8 小时 | 🔴 高 |
| 更新内部引用 | ~20 | 12 小时 | 🟡 中 |
| 编写测试 | ~15 | 20 小时 | 🔴 高 |
| 文档更新 | 5 | 8 小时 | 🟡 中 |
| **总计** | **~53** | **64 小时** | **(8 工作日)** |

---

## 🚨 风险和缓解

### 风险 1: 委托模式性能损失

**风险级别**: 🟢 低

**缓解措施**:
- 委托调用是直接方法转发，JIT 可内联优化
- 基准测试验证 <1% 性能损失
- 热路径代码直接使用新 API

### 风险 2: 遗漏的引用

**风险级别**: 🟡 中

**缓解措施**:
- 使用 IDE 全局搜索验证
- 编译时检查所有 @Deprecated 警告
- 完整的集成测试覆盖

### 风险 3: 第三方代码依赖

**风险级别**: 🟢 低

**缓解措施**:
- 当前无已知第三方使用者
- Maven 发布时明确标注 Breaking Changes
- 提供详细迁移指南

### 风险 4: 文档不同步

**风险级别**: 🟡 中

**缓解措施**:
- 文档与代码同步 PR
- Code Review 检查文档更新
- 自动化文档生成（JavaDoc）

---

## 📅 时间线

| 周次 | 里程碑 | 交付物 |
|------|--------|--------|
| Week 2 | 新类创建完成 | 10 个新 Java 文件 |
| Week 2 | 委托层完成 | 3 个 @Deprecated 类 |
| Week 3 | 内部引用更新 | ~20 个文件修改 |
| Week 3 | 测试完成 | 85%+ 覆盖率 |
| Week 3 | Phase 2 验收 | 所有测试通过 |

---

## 🔗 相关文档

- [UPGRADE_PLAN_V2.md](UPGRADE_PLAN_V2.md) - 完整升级计划
- [dependency-graph.md](dependency-graph.md) - 依赖分析详情
- [MIGRATION_GUIDE_V2.md](MIGRATION_GUIDE_V2.md) - 用户迁移指南
- [API_DESIGN_V2.md](API_DESIGN_V2.md) - API 设计文档

---

**文档版本**: v1.0
**最后更新**: 2025-10-21
**状态**: ✅ 已审核
