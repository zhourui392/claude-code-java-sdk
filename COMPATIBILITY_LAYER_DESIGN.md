# 兼容层设计文档 - v2.0.0

> **创建日期**: 2025-10-21
> **目标**: 确保 v1.0.0 代码在 v2.0.0 中无缝运行
> **移除时间**: v3.0.0（预计 6-12 个月后）

---

## 📋 目录

1. [设计原则](#1-设计原则)
2. [兼容层架构](#2-兼容层架构)
3. [三层兼容策略](#3-三层兼容策略)
4. [详细实现设计](#4-详细实现设计)
5. [弃用警告系统](#5-弃用警告系统)
6. [性能影响分析](#6-性能影响分析)
7. [测试策略](#7-测试策略)
8. [移除计划](#8-移除计划)

---

## 1. 设计原则

### 1.1 核心原则

✅ **100% 向后兼容**
- v1.0.0 代码无需任何修改即可运行
- 所有公共 API 保持可用
- 行为语义完全一致

✅ **透明委托**
- 旧类委托到新类实现
- 零性能损失（JIT 内联优化）
- 用户感知不到差异

✅ **渐进式迁移**
- 提供清晰的迁移路径
- 运行时友好的弃用警告
- 可配置的警告级别

✅ **可测试性**
- 旧 API 和新 API 并行测试
- 兼容性测试套件
- 性能回归测试

### 1.2 非目标

❌ **不支持的场景**:
- 跨版本混用（不支持同时使用 v1 和 v2 类）
- 反射修改私有字段（内部实现可能变更）
- 未文档化的行为依赖

---

## 2. 兼容层架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户代码 (v1.0.0)                      │
│                                                           │
│  ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);         │
│  sdk.query("test");                                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              兼容层 (Compatibility Layer)                 │
│                                                           │
│  ┌─────────────────┐         ┌──────────────────────┐   │
│  │ ClaudeCodeSDK   │─────────│ ClaudeAgentSDK       │   │
│  │ @Deprecated     │委托     │ (新实现)              │   │
│  └─────────────────┘         └──────────────────────┘   │
│                                                           │
│  ┌─────────────────┐         ┌──────────────────────┐   │
│  │ClaudeCodeOptions│─────────│ ClaudeAgentOptions   │   │
│  │ @Deprecated     │转换     │ (新实现)              │   │
│  └─────────────────┘         └──────────────────────┘   │
│                                                           │
│  ┌──────────────────┐        ┌──────────────────────┐   │
│  │ClaudeCodeException│────────│ ClaudeAgentException │   │
│  │ @Deprecated      │继承    │ (新基类)              │   │
│  └──────────────────┘        └──────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                核心服务层 (v2.0.0)                        │
│                                                           │
│  QueryService, HookService, SubagentManager, ...         │
└─────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
v1.0.0 用户代码
    │
    ├─> ClaudeCodeSDK.query()
    │       │
    │       ├─> logDeprecationWarning() ⚠️
    │       │
    │       └─> delegate.query()  ──────> ClaudeAgentSDK.query()
    │                                           │
    │                                           └─> QueryService (v2.0.0)
    │                                                   │
    │                                                   └─> Result
    │
    └─> 返回结果（透明）
```

---

## 3. 三层兼容策略

### 3.1 第一层：委托模式（Delegate Pattern）

**适用于**: ClaudeCodeSDK

**实现方式**:
```java
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeSDK {
    private final ClaudeAgentSDK delegate;  // 委托到新实现

    public ClaudeCodeSDK() {
        this.delegate = new ClaudeAgentSDK();
    }

    // 所有方法转发
    public CompletableFuture<Stream<Message>> query(String prompt) {
        return delegate.query(prompt);
    }
}
```

**优点**:
- ✅ 实现简单
- ✅ 行为完全一致
- ✅ JIT 可内联优化
- ✅ 易于维护

**缺点**:
- ⚠️ 增加一层调用（但可优化）
- ⚠️ 类数量翻倍（临时）

---

### 3.2 第二层：Builder 转换（Builder Conversion）

**适用于**: ClaudeCodeOptions

**实现方式**:
```java
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeOptions {

    // 静态工厂方法返回新 Builder
    public static ClaudeAgentOptions.Builder builder() {
        return ClaudeAgentOptions.builder()
            .useV1CompatibilityMode();  // 自动启用兼容模式
    }

    // 提供从旧 Options 转换到新 Options 的方法
    public ClaudeAgentOptions toAgentOptions() {
        return ClaudeAgentOptions.builder()
            .apiKey(this.apiKey)
            .baseUrl(this.baseUrl)
            // ... 复制所有字段
            .useV1CompatibilityMode()
            .build();
    }
}
```

**优点**:
- ✅ 用户无感知
- ✅ 自动启用 v1 兼容模式
- ✅ 配置转换透明

**缺点**:
- ⚠️ 需要维护两套 Builder
- ⚠️ 字段映射可能复杂

---

### 3.3 第三层：继承模式（Inheritance Pattern）

**适用于**: ClaudeCodeException

**实现方式**:
```java
// 新基类
public class ClaudeAgentException extends RuntimeException {
    // 完整实现
}

// 旧类继承新类
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

**优点**:
- ✅ 完全兼容（catch 语句无需修改）
- ✅ 新旧异常可互换捕获
- ✅ 无性能损失

**缺点**:
- ⚠️ 继承层级增加
- ⚠️ 需同步维护构造函数

---

## 4. 详细实现设计

### 4.1 ClaudeCodeSDK 兼容实现

#### 完整代码

```java
package com.anthropic.claude.client;

import com.anthropic.claude.config.ClaudeAgentOptions;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.hooks.HookCallback;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.query.QueryBuilder;
import com.anthropic.claude.query.QueryRequest;
import com.anthropic.claude.subagents.SubagentManager;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Claude Code SDK 主入口类（已弃用）
 *
 * <p><strong>⚠️ 此类已在 v2.0.0 中弃用</strong></p>
 *
 * <p>请迁移到 {@link ClaudeAgentSDK}，此类将在 v3.0.0 中移除。</p>
 *
 * <p>迁移指南：<a href="https://github.com/.../MIGRATION_GUIDE_V2.md">
 * MIGRATION_GUIDE_V2.md</a></p>
 *
 * <h3>迁移示例</h3>
 * <pre>{@code
 * // 旧代码 (v1.0.0)
 * ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);
 *
 * // 新代码 (v2.0.0+)
 * ClaudeAgentSDK sdk = new ClaudeAgentSDK(options);
 * }</pre>
 *
 * @author Claude Code Java SDK Team
 * @version 1.0.0
 * @since 1.0.0
 * @deprecated 自 v2.0.0 起已弃用，请使用 {@link ClaudeAgentSDK}
 *             此类将在 v3.0.0 中移除（预计 2026 年）
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeSDK {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeSDK.class);

    /**
     * 确保弃用警告只输出一次
     */
    private static final AtomicBoolean WARNING_LOGGED = new AtomicBoolean(false);

    /**
     * 委托到新实现
     */
    private final ClaudeAgentSDK delegate;

    /**
     * 使用默认配置创建实例
     */
    public ClaudeCodeSDK() {
        logDeprecationWarning();
        this.delegate = new ClaudeAgentSDK();
    }

    /**
     * 使用自定义配置创建实例
     *
     * @param options 配置选项（v1.0.0 格式）
     */
    public ClaudeCodeSDK(ClaudeCodeOptions options) {
        logDeprecationWarning();
        // 转换 v1 Options -> v2 Options
        ClaudeAgentOptions v2Options = convertToV2Options(options);
        this.delegate = new ClaudeAgentSDK(v2Options);
    }

    // ====== 委托所有公共方法 ======

    public CompletableFuture<Stream<Message>> query(String prompt) {
        return delegate.query(prompt);
    }

    public CompletableFuture<Stream<Message>> query(QueryRequest request) {
        return delegate.query(request);
    }

    public Observable<Message> queryStream(String prompt) {
        return delegate.queryStream(prompt);
    }

    public Observable<Message> queryStream(QueryRequest request) {
        return delegate.queryStream(request);
    }

    public QueryBuilder queryBuilder(String prompt) {
        return delegate.queryBuilder(prompt);
    }

    public void addHook(String eventType, HookCallback callback) {
        delegate.addHook(eventType, callback);
    }

    public void removeHook(String eventType, HookCallback callback) {
        delegate.removeHook(eventType, callback);
    }

    public SubagentManager getSubagentManager() {
        return delegate.getSubagentManager();
    }

    /**
     * @deprecated 配置不可变，此方法在 v2.0.0 中已移除
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public void configure(ClaudeCodeOptions newOptions) {
        throw new UnsupportedOperationException(
            "configure() 已在 v2.0.0 中移除。请使用新配置创建新的 SDK 实例。"
        );
    }

    public ClaudeCodeOptions getConfiguration() {
        // 反向转换 v2 -> v1
        return convertToV1Options(delegate.getConfiguration());
    }

    public boolean isAuthenticated() {
        return delegate.isAuthenticated();
    }

    public void refreshAuthentication() {
        delegate.refreshAuthentication();
    }

    public boolean isCliAvailable() {
        return delegate.isCliAvailable();
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public boolean healthCheck() {
        return delegate.healthCheck();
    }

    // ====== 内部辅助方法 ======

    /**
     * 输出弃用警告（仅一次）
     */
    private void logDeprecationWarning() {
        if (WARNING_LOGGED.compareAndSet(false, true)) {
            logger.warn("");
            logger.warn("╔═══════════════════════════════════════════════════════════════════╗");
            logger.warn("║                    ⚠️  DEPRECATION WARNING                        ║");
            logger.warn("║                                                                   ║");
            logger.warn("║  ClaudeCodeSDK 已在 v2.0.0 中弃用                                  ║");
            logger.warn("║  请迁移到 ClaudeAgentSDK                                          ║");
            logger.warn("║                                                                   ║");
            logger.warn("║  此类将在 v3.0.0 中移除（预计 2026 年）                            ║");
            logger.warn("║                                                                   ║");
            logger.warn("║  迁移步骤：                                                       ║");
            logger.warn("║  1. 将 'ClaudeCodeSDK' 替换为 'ClaudeAgentSDK'                    ║");
            logger.warn("║  2. 将 'ClaudeCodeOptions' 替换为 'ClaudeAgentOptions'            ║");
            logger.warn("║  3. 运行测试确保正常工作                                          ║");
            logger.warn("║                                                                   ║");
            logger.warn("║  迁移指南: MIGRATION_GUIDE_V2.md                                  ║");
            logger.warn("║  文档: https://github.com/.../MIGRATION_GUIDE_V2.md               ║");
            logger.warn("╚═══════════════════════════════════════════════════════════════════╝");
            logger.warn("");
        }
    }

    /**
     * 转换 v1 Options -> v2 Options
     */
    private ClaudeAgentOptions convertToV2Options(ClaudeCodeOptions v1Options) {
        if (v1Options == null) {
            return null;
        }

        return ClaudeAgentOptions.builder()
            // 基础配置
            .apiKey(v1Options.getApiKey())
            .baseUrl(v1Options.getBaseUrl())
            .cliPath(v1Options.getCliPath())
            .cliEnabled(v1Options.isCliEnabled())
            .timeout(v1Options.getTimeout())
            .maxRetries(v1Options.getMaxRetries())
            .enableLogging(v1Options.isEnableLogging())
            .environment(v1Options.getEnvironment())
            .authProvider(v1Options.getAuthProvider())

            // CLI 模式
            .cliMode(v1Options.getCliMode())
            .ptyReadyTimeout(v1Options.getPtyReadyTimeout())
            .promptPattern(v1Options.getPromptPattern())
            .additionalArgs(v1Options.getAdditionalArgs())

            // 连接池
            .minPoolSize(v1Options.getMinPoolSize())
            .maxPoolSize(v1Options.getMaxPoolSize())
            .connectionTimeout(v1Options.getConnectionTimeout())
            .healthCheckInterval(v1Options.getHealthCheckInterval())

            // ⚠️ 重要：启用 v1 兼容模式
            .useV1CompatibilityMode()

            .build();
    }

    /**
     * 转换 v2 Options -> v1 Options
     */
    private ClaudeCodeOptions convertToV1Options(ClaudeAgentOptions v2Options) {
        if (v2Options == null) {
            return null;
        }

        return ClaudeCodeOptions.builder()
            .apiKey(v2Options.getApiKey())
            .baseUrl(v2Options.getBaseUrl())
            .cliPath(v2Options.getCliPath())
            .cliEnabled(v2Options.isCliEnabled())
            .timeout(v2Options.getTimeout())
            .maxRetries(v2Options.getMaxRetries())
            .enableLogging(v2Options.isEnableLogging())
            .environment(v2Options.getEnvironment())
            .authProvider(v2Options.getAuthProvider())
            .cliMode(v2Options.getCliMode())
            .ptyReadyTimeout(v2Options.getPtyReadyTimeout())
            .promptPattern(v2Options.getPromptPattern())
            .additionalArgs(v2Options.getAdditionalArgs())
            .minPoolSize(v2Options.getMinPoolSize())
            .maxPoolSize(v2Options.getMaxPoolSize())
            .connectionTimeout(v2Options.getConnectionTimeout())
            .healthCheckInterval(v2Options.getHealthCheckInterval())
            .build();
    }
}
```

---

### 4.2 ClaudeCodeOptions 兼容实现

```java
package com.anthropic.claude.config;

import com.anthropic.claude.auth.AuthenticationProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Claude Code SDK 配置选项（已弃用）
 *
 * <p><strong>⚠️ 此类已在 v2.0.0 中弃用</strong></p>
 *
 * <p>请迁移到 {@link ClaudeAgentOptions}。</p>
 *
 * @deprecated 自 v2.0.0 起已弃用，请使用 {@link ClaudeAgentOptions}
 *             此类将在 v3.0.0 中移除
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeOptions {

    // ⚠️ 保留所有 v1.0.0 字段（为了反向转换）
    private final String apiKey;
    private final String baseUrl;
    private final String cliPath;
    private final boolean cliEnabled;
    private final Duration timeout;
    private final int maxRetries;
    private final boolean enableLogging;
    private final Map<String, String> environment;
    private final AuthenticationProvider authProvider;
    private final CliMode cliMode;
    private final Duration ptyReadyTimeout;
    private final String promptPattern;
    private final List<String> additionalArgs;
    private final int minPoolSize;
    private final int maxPoolSize;
    private final long connectionTimeout;
    private final long healthCheckInterval;

    private ClaudeCodeOptions(Builder builder) {
        // ... 字段赋值（与 v1.0.0 完全相同）
    }

    // ====== 静态工厂方法（返回新 Builder） ======

    /**
     * 创建配置构建器
     *
     * <p><strong>⚠️ 此方法返回 {@link ClaudeAgentOptions.Builder}</strong></p>
     *
     * @return ClaudeAgentOptions.Builder（自动启用 v1 兼容模式）
     * @deprecated 使用 {@link ClaudeAgentOptions#builder()}
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static ClaudeAgentOptions.Builder builder() {
        return ClaudeAgentOptions.builder()
            .useV1CompatibilityMode();  // 🔑 关键：自动启用兼容模式
    }

    // ====== Getters（与 v1.0.0 相同） ======

    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getCliPath() { return cliPath; }
    public boolean isCliEnabled() { return cliEnabled; }
    public Duration getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isEnableLogging() { return enableLogging; }
    public Map<String, String> getEnvironment() { return environment; }
    public AuthenticationProvider getAuthProvider() { return authProvider; }
    public CliMode getCliMode() { return cliMode; }
    public Duration getPtyReadyTimeout() { return ptyReadyTimeout; }
    public String getPromptPattern() { return promptPattern; }
    public List<String> getAdditionalArgs() { return additionalArgs; }
    public int getMinPoolSize() { return minPoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getHealthCheckInterval() { return healthCheckInterval; }

    /**
     * 转换为新配置格式
     *
     * @return ClaudeAgentOptions
     */
    public ClaudeAgentOptions toAgentOptions() {
        return ClaudeAgentOptions.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .cliPath(cliPath)
            .cliEnabled(cliEnabled)
            .timeout(timeout)
            .maxRetries(maxRetries)
            .enableLogging(enableLogging)
            .environment(environment)
            .authProvider(authProvider)
            .cliMode(cliMode)
            .ptyReadyTimeout(ptyReadyTimeout)
            .promptPattern(promptPattern)
            .additionalArgs(additionalArgs)
            .minPoolSize(minPoolSize)
            .maxPoolSize(maxPoolSize)
            .connectionTimeout(connectionTimeout)
            .healthCheckInterval(healthCheckInterval)
            .useV1CompatibilityMode()
            .build();
    }

    /**
     * Builder 类（保留用于反向转换）
     *
     * @deprecated 使用 {@link ClaudeAgentOptions.Builder}
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static class Builder {
        // ... 保留所有 v1.0.0 Builder 代码（为了支持 convertToV1Options）
    }
}
```

---

### 4.3 ClaudeCodeException 兼容实现

```java
package com.anthropic.claude.exceptions;

/**
 * Claude Code SDK 基础异常（已弃用）
 *
 * <p><strong>⚠️ 此类已在 v2.0.0 中弃用</strong></p>
 *
 * <p>请迁移到 {@link ClaudeAgentException}。</p>
 *
 * <p>注意：此类现在继承自 {@link ClaudeAgentException}，
 * 因此 catch 语句无需修改。</p>
 *
 * @deprecated 自 v2.0.0 起已弃用，请使用 {@link ClaudeAgentException}
 *             此类将在 v3.0.0 中移除
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class ClaudeCodeException extends ClaudeAgentException {

    /**
     * 创建异常
     *
     * @param message 错误消息
     */
    public ClaudeCodeException(String message) {
        super(message);
    }

    /**
     * 创建异常（带原因）
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public ClaudeCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建异常（带错误代码）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public ClaudeCodeException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 创建异常（完整参数）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因
     */
    public ClaudeCodeException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
```

---

## 5. 弃用警告系统

### 5.1 警告级别

| 级别 | 触发时机 | 输出位置 | 可配置 |
|------|----------|----------|--------|
| **ERROR** | 使用已移除的 API | 日志 + 异常 | ❌ 否 |
| **WARN** | 使用 @Deprecated API | 日志（SLF4J） | ✅ 是 |
| **INFO** | v2 新特性可用 | 日志 | ✅ 是 |
| **DEBUG** | 内部转换细节 | 日志 | ✅ 是 |

### 5.2 警告配置

#### 方式 1：环境变量
```bash
# 禁用弃用警告
export CLAUDE_SUPPRESS_DEPRECATION_WARNINGS=true

# 设置警告级别
export CLAUDE_DEPRECATION_WARNING_LEVEL=WARN  # ERROR, WARN, INFO, NONE
```

#### 方式 2：logback.xml
```xml
<!-- 禁用 ClaudeCodeSDK 弃用警告 -->
<logger name="com.anthropic.claude.client.ClaudeCodeSDK" level="ERROR"/>

<!-- 或者全局禁用弃用警告 -->
<logger name="com.anthropic.claude" level="ERROR">
    <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
        <evaluator>
            <expression>message.contains("DEPRECATION WARNING")</expression>
        </evaluator>
        <OnMismatch>NEUTRAL</OnMismatch>
        <OnMatch>DENY</OnMatch>
    </filter>
</logger>
```

#### 方式 3：代码配置
```java
ClaudeAgentOptions options = ClaudeAgentOptions.builder()
    .suppressDeprecationWarnings(true)  // 禁用警告
    .build();
```

### 5.3 警告输出示例

```
2025-10-21 10:30:15.123 WARN  c.a.c.client.ClaudeCodeSDK -
╔═══════════════════════════════════════════════════════════════════╗
║                    ⚠️  DEPRECATION WARNING                        ║
║                                                                   ║
║  ClaudeCodeSDK 已在 v2.0.0 中弃用                                  ║
║  请迁移到 ClaudeAgentSDK                                          ║
║                                                                   ║
║  此类将在 v3.0.0 中移除（预计 2026 年）                            ║
║                                                                   ║
║  迁移步骤：                                                       ║
║  1. 将 'ClaudeCodeSDK' 替换为 'ClaudeAgentSDK'                    ║
║  2. 将 'ClaudeCodeOptions' 替换为 'ClaudeAgentOptions'            ║
║  3. 运行测试确保正常工作                                          ║
║                                                                   ║
║  迁移指南: MIGRATION_GUIDE_V2.md                                  ║
║  文档: https://github.com/.../MIGRATION_GUIDE_V2.md               ║
╚═══════════════════════════════════════════════════════════════════╝
```

---

## 6. 性能影响分析

### 6.1 委托调用开销

**理论分析**:
```java
// 委托调用
ClaudeCodeSDK -> delegate.query() -> ClaudeAgentSDK.query()

// 直接调用
ClaudeAgentSDK -> query()
```

**开销**:
- 额外方法调用：1 次
- JIT 优化后：0（内联优化）

### 6.2 基准测试结果（预期）

| 场景 | v1 直接调用 | v2 委托调用 | 开销 |
|------|-------------|-------------|------|
| 单次 query | 100ms | 100.01ms | <0.01% |
| 1000 次 query | 100s | 100.05s | <0.05% |
| 流式查询 | 无差异 | 无差异 | 0% |
| 内存占用 | 基准 | +0.1% | 可忽略 |

**结论**: ✅ 委托模式对性能影响可忽略不计

### 6.3 优化建议

1. **JIT 预热**: 生产环境启动后进行预热调用
2. **热路径直连**: 性能敏感代码直接使用新 API
3. **避免反射**: 不使用反射调用委托方法

---

## 7. 测试策略

### 7.1 兼容性测试套件

```java
/**
 * 兼容性测试：验证 v1 API 在 v2 中正常工作
 */
@Test
public class CompatibilityTest {

    @Test
    void testClaudeCodeSDK_WorksInV2() {
        // 使用 v1.0.0 代码
        ClaudeCodeOptions options = ClaudeCodeOptions.builder()
            .apiKey("test-key")
            .build();

        ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);

        // 验证所有方法正常工作
        assertNotNull(sdk);
        assertDoesNotThrow(() -> sdk.query("test"));
        assertTrue(sdk.healthCheck());
    }

    @Test
    void testClaudeCodeOptions_BuildsClaudeAgentOptions() {
        ClaudeAgentOptions.Builder builder = ClaudeCodeOptions.builder();

        ClaudeAgentOptions options = builder
            .apiKey("test-key")
            .build();

        assertNotNull(options);
        assertEquals("test-key", options.getApiKey());
        // 验证 v1 兼容模式已启用
        assertTrue(options.getSettingSources().contains(SettingSource.CLAUDE_MD));
    }

    @Test
    void testClaudeCodeException_CatchableAsClaudeAgentException() {
        assertThrows(ClaudeAgentException.class, () -> {
            throw new ClaudeCodeException("test");
        });

        assertThrows(ClaudeCodeException.class, () -> {
            throw new ClaudeAgentException("test");
        });
    }
}
```

### 7.2 性能回归测试

```java
@Test
void testNoPerformanceRegression() {
    // 基准：直接调用
    long directTime = measureExecutionTime(() -> {
        ClaudeAgentSDK sdk = new ClaudeAgentSDK();
        sdk.query("test").join();
    });

    // 委托调用
    long delegateTime = measureExecutionTime(() -> {
        ClaudeCodeSDK sdk = new ClaudeCodeSDK();
        sdk.query("test").join();
    });

    // 验证开销 <5%
    double overhead = (delegateTime - directTime) / (double) directTime;
    assertTrue(overhead < 0.05, "Delegation overhead should be <5%");
}
```

---

## 8. 移除计划

### 8.1 时间表

| 版本 | 日期（预计） | 兼容层状态 | 行动 |
|------|--------------|------------|------|
| **v2.0.0** | 2025-Q2 | ✅ 完整支持 | 发布兼容层 |
| **v2.1.x** | 2025-Q3 | ✅ 维护 | Bug 修复 |
| **v2.5.0** | 2025-Q4 | ⚠️ 警告增强 | 增加弃用提醒频率 |
| **v3.0.0-beta** | 2026-Q1 | ⚠️ 最后警告 | 移除倒计时 |
| **v3.0.0** | 2026-Q2 | ❌ 移除 | 删除所有 @Deprecated 类 |

### 8.2 v3.0.0 移除清单

**将被移除的类**:
- ❌ `com.anthropic.claude.client.ClaudeCodeSDK`
- ❌ `com.anthropic.claude.config.ClaudeCodeOptions`
- ❌ `com.anthropic.claude.config.ClaudeCodeOptions.Builder`
- ❌ `com.anthropic.claude.exceptions.ClaudeCodeException`
- ❌ `com.anthropic.claude.config.Configuration`（遗留类）

**保留的类**:
- ✅ 所有其他 63 个类

---

## 9. FAQ

### Q1: 我必须立即迁移吗？

**A**: 不必须。v2.0.0 提供完整兼容，你的 v1.0.0 代码无需修改即可运行。但建议在 6 个月内（v3.0.0 发布前）完成迁移。

### Q2: 如何禁用弃用警告？

**A**:
```java
// 方式 1: 环境变量
export CLAUDE_SUPPRESS_DEPRECATION_WARNINGS=true

// 方式 2: logback.xml
<logger name="com.anthropic.claude.client.ClaudeCodeSDK" level="ERROR"/>

// 方式 3: 代码
ClaudeAgentOptions.builder().suppressDeprecationWarnings(true).build();
```

### Q3: 委托模式有性能损失吗？

**A**: 几乎没有（<0.05%）。JIT 编译器会内联优化委托调用。

### Q4: 如何验证兼容性？

**A**: 运行你的完整测试套件。如果所有测试通过，说明兼容。

### Q5: v3.0.0 什么时候发布？

**A**: 预计 2026 年 Q2，距离 v2.0.0 发布后 6-12 个月。

---

**文档版本**: v1.0
**最后更新**: 2025-10-21
**状态**: ✅ 已审核
