# SDK 执行模式技术方案（默认 --print）

## 背景与目标
- 在 SDK 内同时支持两种 CLI 执行模式：
  - 批处理模式（默认，稳定）：每次调用启动 CLI，读取输出后进程结束；默认采用 `--print`。
  - 交互（PTY）模式（可选）：维护常驻会话，向 stdin 写入，按行监听 stdout。
- Windows 11 默认批处理；交互模式在异常时自动回退至批处理。

## 架构设计（策略模式）
- 新增接口 `CliExecutionStrategy`：
  - `start()`, `shutdown()`, `Stream<Message> execute(QueryRequest)`, `Observable<Message> executeStream(QueryRequest)`。
- 策略一 `BatchProcessStrategy`（现有逻辑归档）：
  - 复用 `ProcessManager` 构建命令；标准：`--print` 或 `--output-format json`；流式：`--output-format json-stream --stream`。
- 策略二 `PtyInteractiveStrategy`（新）：
  - 组合 `PtyManager` + `OutputParser`；行切分兼容 `\r\n|\n|\r`；就绪采用“首次活跃输出/生存阈值”。
  - 就绪超时抛错，触发回退到批处理。

## 配置扩展（向后兼容）
- `ClaudeCodeOptions` 新增：
  - `CliMode { BATCH, PTY_INTERACTIVE }`，默认 `BATCH`。
  - `ptyReadyTimeout: Duration`，`promptPattern: String`，`additionalArgs: List<String>`。
- `ClaudeCodeSDK.shutdown()` 释放 PTY 资源（仅在交互模式）。

示例（设计说明，不立即编码）：
```java
ClaudeCodeOptions options = ClaudeCodeOptions.builder()
    .cliMode(CliMode.BATCH) // 默认（--print）
    .ptyReadyTimeout(Duration.ofSeconds(10))
    .build();
```

## 命令与参数规范
- 批处理（默认）：
  - 标准输出：`--print`（或 `--output-format json`）
  - 流式输出：`--output-format json-stream --stream`
- 交互（PTY）：
  - 不强制 `--print`；按次写入 stdin；必要时可通过配置注入 `additionalArgs`。

## 行为与回退
- 交互模式就绪判定：活跃输出或生存超阈值即就绪；若超时抛错并回退批处理。
- 行与编码：统一 UTF-8；解析按 `\r\n|\n|\r` 拆分；仅写入端追加一次换行，避免双换行。
- 会话保持：通过 `--continue`/`--resume <id>` 在两种模式下均可实现。

## 平台与路径（Windows 11）
- 路径解析：`claude.cmd` 优先；必要时 `cmd /c` 包裹。
- 环境：`$env:ANTHROPIC_API_KEY`、`$env:ANTHROPIC_BASE_URL`。

## 测试计划
- 单测：命令构建（含 `--print`/json-stream）、CR/LF 分割、PTY 就绪超时回退。
- 集成：Windows 上批处理稳定性；交互模式启动/回退路径。

## 渐进落地
1) 引入枚举与策略接口；
2) 迁移现有批处理逻辑至 `BatchProcessStrategy`；
3) 实现 `PtyInteractiveStrategy` 与回退；
4) 文档与示例完善。

> 说明：本方案已写入仓库，默认 `--print` 模式；当前不开始编码实现。
