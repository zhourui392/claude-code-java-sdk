# CLI 模式切换完整实现指南

## 📍 CLI 模式切换的实现位置

CLI模式切换已经在以下地方完全实现：

### 1. 核心配置类
- **`ClaudeCodeOptions.Builder`** - 代码配置入口
- **`ConfigLoader`** - 配置文件和环境变量支持
- **`CliExecutionStrategyFactory`** - 策略工厂自动选择

### 2. 策略实现类
- **`BatchProcessStrategy`** - 批处理模式实现
- **`PtyInteractiveStrategy`** - PTY交互模式实现
- **`QueryService`** - 运行时策略管理

---

## 🚀 实际使用方法

### 方法1: 代码中直接配置

```java
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.config.CliMode;
import com.anthropic.claude.client.ClaudeCodeSDK;
import java.time.Duration;

// 1. 批处理模式（默认，最稳定）
ClaudeCodeOptions batchOptions = ClaudeCodeOptions.builder()
    .cliMode(CliMode.BATCH)
    .timeout(Duration.ofMinutes(5))
    .build();

// 2. PTY交互模式（更快响应）
ClaudeCodeOptions ptyOptions = ClaudeCodeOptions.builder()
    .cliMode(CliMode.PTY_INTERACTIVE)
    .ptyReadyTimeout(Duration.ofSeconds(15))
    .promptPattern("claude>.*")
    .addAdditionalArg("--verbose")
    .addAdditionalArg("--debug")
    .build();

// 3. 创建SDK实例
ClaudeCodeSDK sdk = new ClaudeCodeSDK(ptyOptions);

// 4. 查询会自动使用配置的模式
sdk.query("Hello Claude").thenAccept(messages -> {
    messages.forEach(msg -> System.out.println(msg.getContent()));
});
```

### 方法2: 配置文件切换

**用户配置文件** `~/.claude/config.properties`:
```properties
# 设置CLI模式
cli.mode=PTY_INTERACTIVE

# PTY模式配置
pty.ready.timeout=15000
prompt.pattern=claude>.*

# 额外参数（用逗号分隔）
additional.args=--verbose,--debug,--stream

# 其他配置
timeout.seconds=300
max.retries=5
```

**项目配置文件** `src/main/resources/claude-code.properties`:
```properties
# 项目默认使用批处理模式
cli.mode=BATCH

# 基础配置
timeout.seconds=600
max.retries=3
logging.enabled=true
```

### 方法3: 环境变量切换

```bash
# Windows CMD
set CLAUDE_CODE_CLI_MODE=PTY_INTERACTIVE
set CLAUDE_CODE_PTY_READY_TIMEOUT=15000
set CLAUDE_CODE_PROMPT_PATTERN=claude>.*
set CLAUDE_CODE_ADDITIONAL_ARGS=--verbose,--debug

# Windows PowerShell
$env:CLAUDE_CODE_CLI_MODE="PTY_INTERACTIVE"
$env:CLAUDE_CODE_PTY_READY_TIMEOUT="15000"
$env:CLAUDE_CODE_PROMPT_PATTERN="claude>.*"
$env:CLAUDE_CODE_ADDITIONAL_ARGS="--verbose,--debug"

# Linux/macOS Bash
export CLAUDE_CODE_CLI_MODE=PTY_INTERACTIVE
export CLAUDE_CODE_PTY_READY_TIMEOUT=15000
export CLAUDE_CODE_PROMPT_PATTERN="claude>.*"
export CLAUDE_CODE_ADDITIONAL_ARGS="--verbose,--debug"
```

### 方法4: 使用ConfigLoader自动配置

```java
import com.anthropic.claude.config.ConfigLoader;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.client.ClaudeCodeSDK;

// ConfigLoader自动从多个来源加载配置
ConfigLoader configLoader = new ConfigLoader();
ClaudeCodeOptions options = configLoader.createOptions();

// 检查最终配置
System.out.println("使用CLI模式: " + options.getCliMode());
System.out.println("PTY超时: " + options.getPtyReadyTimeout());

// 创建SDK - 配置已自动应用
ClaudeCodeSDK sdk = new ClaudeCodeSDK(options);
```

---

## 🔄 运行时动态切换

### 高级用法: 条件切换

```java
public class SmartCliManager {

    public ClaudeCodeOptions getOptimalConfiguration() {
        // 1. 检测运行环境
        if (isDockerEnvironment()) {
            return ClaudeCodeOptions.builder()
                .cliMode(CliMode.BATCH)
                .build();
        }

        // 2. Windows环境优化
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return ClaudeCodeOptions.builder()
                .cliMode(CliMode.PTY_INTERACTIVE)
                .ptyReadyTimeout(Duration.ofSeconds(10))
                .addAdditionalArg("--verbose")
                .build();
        }

        // 3. 默认批处理模式
        return ClaudeCodeOptions.builder()
            .cliMode(CliMode.BATCH)
            .build();
    }

    private boolean isDockerEnvironment() {
        return System.getenv("DOCKER_CONTAINER") != null ||
               new File("/.dockerenv").exists();
    }
}
```

### 性能监控切换

```java
public class PerformanceAwareManager {
    private final MetricsCollector metrics;
    private CliMode currentMode = CliMode.BATCH;

    public void evaluatePerformance() {
        double avgResponseTime = metrics.getAverageResponseTime();
        double errorRate = metrics.getErrorRate();

        if (errorRate > 0.1) {
            // 错误率高，切换到稳定的批处理模式
            switchToMode(CliMode.BATCH, "高错误率");
        } else if (avgResponseTime < 1000 && errorRate < 0.01) {
            // 性能良好，可以使用PTY模式
            switchToMode(CliMode.PTY_INTERACTIVE, "性能优秀");
        }
    }

    private void switchToMode(CliMode mode, String reason) {
        if (currentMode != mode) {
            logger.info("切换CLI模式: {} -> {} (原因: {})",
                currentMode, mode, reason);
            currentMode = mode;
            // 重新创建SDK配置...
        }
    }
}
```

---

## 📊 配置优先级说明

配置按以下优先级应用（高优先级覆盖低优先级）：

1. **代码显式配置** - `ClaudeCodeOptions.builder().cliMode(...)`
2. **环境变量** - `CLAUDE_CODE_CLI_MODE=PTY_INTERACTIVE`
3. **用户配置** - `~/.claude/config.properties`
4. **项目配置** - `src/main/resources/claude-code.properties`
5. **默认值** - `CliMode.BATCH`

---

## 🎯 最佳实践建议

### 开发环境
```properties
# ~/.claude/config.properties
cli.mode=PTY_INTERACTIVE
pty.ready.timeout=10000
additional.args=--verbose,--debug
```

### 生产环境
```properties
# application.properties
cli.mode=BATCH
timeout.seconds=300
max.retries=3
```

### CI/CD环境
```bash
# 在CI脚本中设置
export CLAUDE_CODE_CLI_MODE=BATCH
export CLAUDE_CODE_TIMEOUT_SECONDS=600
```

### 容器环境
```dockerfile
# Dockerfile
ENV CLAUDE_CODE_CLI_MODE=BATCH
ENV CLAUDE_CODE_TIMEOUT_SECONDS=300
```

---

## 🔍 验证配置是否生效

```java
// 验证配置加载
ConfigLoader loader = new ConfigLoader();
System.out.println("CLI模式: " + loader.getCliMode());
System.out.println("PTY超时: " + loader.getPtyReadyTimeout());
System.out.println("提示符模式: " + loader.getPromptPattern());

// 验证策略创建
ClaudeCodeOptions options = loader.createOptions();
// 策略工厂会根据配置自动选择正确的策略实现
```

---

## ⚠️ 故障排除

### 常见问题

1. **PTY模式启动失败**
   - 自动回退到批处理模式
   - 检查CLI路径是否正确
   - 确认终端支持PTY

2. **配置不生效**
   - 检查配置文件路径
   - 验证环境变量名称
   - 查看日志输出

3. **性能问题**
   - 批处理模式: 稳定但响应较慢
   - PTY模式: 快速但可能不稳定

通过以上配置方式，你可以在任何环境中灵活切换CLI执行模式，获得最佳的性能和稳定性平衡。