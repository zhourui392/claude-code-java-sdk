# Repository Guidelines

## 项目结构与模块组织
- 根工程：多模块 Maven（Java 17）。
- `claude-code-java-sdk/`：核心 SDK（`src/main/java`、`src/test/java`、`src/main/resources`）。示例：`com.anthropic.claude.*`。
- `claude-code-gui/`：Swing GUI（入口：`com.claude.gui.ClaudeCodeGUI`），Shade 产物在 `target/`。
- 文档与说明：根目录 `spec.md`、`todolist.md`；SDK 内 `README.md`、`BASE_URL_USAGE.md` 等。

## 构建、测试与本地运行（Windows 11）
- 全量构建（根目录）：`mvn -q clean install`
- 仅构建 SDK：`mvn -q -pl claude-code-java-sdk -am clean install`
- 运行 GUI：`java -jar .\claude-code-gui\target\claude-code-gui-1.0.0.jar`（如存在 `*-shaded.jar` 优先使用）
- 全量测试：`mvn -q test`；单测：`mvn -q -Dtest=ClassNameTest test`；失败聚合：`mvn -q -fae test`

## 代码风格与命名约定
- 语言与编码：Java 17，UTF-8，缩进 4 空格；建议单行不超 120 列。
- 命名：类/接口 PascalCase；方法/变量 camelCase；常量 UPPER_SNAKE_CASE；包名小写且分层。
- 规范：遵循 Alibaba-P3C；仓库未启用强制校验，建议在 IDE 安装 P3C 插件并自检。

## 测试规范
- 框架：JUnit 5 + Mockito（见各模块 `pom.xml`）。
- 位置与命名：`src/test/java` 下以 `*Test.java` 结尾（例如 `ProcessManagerTest`）。
- 覆盖率：当前未配置强制门槛或报告，如需可自行引入 JaCoCo（建议但非强制）。

## 提交与 Pull Request
- 提交信息：当前历史无统一约定（`git log` 可见）。建议使用简明动词 + 范围，或参考 Conventional Commits（建议）。
- PR 要求：
  - 说明动机与变更点，关联 Issue（如有）。
  - GUI 相关附截图/录屏；影响面与兼容性说明。
  - 覆盖或更新相应单测；本地通过 `mvn -q test`。

## 安全与配置提示
- 环境变量（优先级最高）：`ANTHROPIC_BASE_URL`、`ANTHROPIC_API_KEY`、`ANTHROPIC_AUTH_TOKEN`。
  - PowerShell 示例：`$env:ANTHROPIC_BASE_URL="https://api.example.com"`。
- 配置文件：`~/.claude/config.properties` 与 `claude-code-java-sdk/src/main/resources/claude-code.properties`（详见 `BASE_URL_USAGE.md`）。

## 面向 Agent 的说明
- 仅修改与任务相关的最小范围；遵循模块边界，不跨模块引入耦合。
- 变更前阅读对应模块 `pom.xml` 与包结构；提交前在根目录运行 `mvn -q test` 并修复失败后再提交。
