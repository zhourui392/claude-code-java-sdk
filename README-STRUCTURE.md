# Claude Code 多模块项目

本项目已重新组织为多模块Maven项目，包含两个子模块：

## 项目结构

```
claude-code-parent/
├── pom.xml                    # 父级POM，统一管理依赖版本
├── claude-code-java-sdk/      # Claude Code Java SDK 模块
│   ├── src/                   # SDK源代码
│   ├── target/                # 编译产物
│   └── pom.xml               # SDK模块POM
├── claude-code-gui/           # Claude Code GUI 模块
│   ├── src/                   # GUI源代码
│   ├── target/                # 编译产物
│   └── pom.xml               # GUI模块POM
└── README-STRUCTURE.md        # 本文档
```

## 模块说明

### 1. claude-code-java-sdk
- **GroupId**: `com.anthropic`
- **ArtifactId**: `claude-code-java-sdk`
- **功能**: Claude Code的Java SDK实现
- **依赖**: HTTP客户端、JSON处理、响应式编程等

### 2. claude-code-gui
- **GroupId**: `com.anthropic.claude`
- **ArtifactId**: `claude-code-gui`
- **功能**: 基于Swing的GUI界面
- **依赖**: claude-code-java-sdk + GUI组件库
- **输出**: 可执行的Fat JAR

## 构建命令

### 编译整个项目
```bash
cd D:\agent_workspace
mvn clean compile
```

### 运行测试
```bash
# 测试整个项目
mvn test

# 只测试SDK模块
cd claude-code-java-sdk
mvn test
```

### 构建可执行JAR
```bash
# 构建GUI可执行JAR
cd claude-code-gui
mvn package

# 生成的JAR位置
# ./target/claude-code-gui-1.0.0.jar (Fat JAR，包含所有依赖)
```

### 运行GUI应用
```bash
cd claude-code-gui/target
java -jar claude-code-gui-1.0.0.jar
```

## 开发工作流

### 1. 修改SDK代码
```bash
cd claude-code-java-sdk
# 修改代码...
mvn test  # 运行SDK测试
```

### 2. 修改GUI代码
```bash
cd claude-code-gui
# 修改代码...
mvn compile  # 编译
mvn package  # 构建可执行JAR
```

### 3. 同时开发两个模块
```bash
# 在根目录执行，会按依赖顺序构建
mvn clean compile test
```

## 优势

1. **模块分离**: SDK和GUI独立开发，职责清晰
2. **依赖管理**: 父POM统一管理版本，避免冲突
3. **构建效率**: 可以单独构建和测试各模块
4. **易于调试**: 不需要每次都启动整个项目
5. **部署灵活**: 可以单独发布SDK或GUI

## IDE支持

在IntelliJ IDEA中：
1. 打开根目录 `D:\agent_workspace`
2. IDEA会自动识别多模块Maven项目
3. 可以在Project窗口看到两个模块
4. 支持模块间跳转和调试

## 解决的问题

✅ **CLI调用问题**: 已修复`ClaudeCodeSDK.query()`方法实现
✅ **配置问题**: GUI执行器中已启用CLI调用 (`cliEnabled(true)`)
✅ **项目结构**: 重新组织为多模块项目，便于开发和调试
✅ **依赖管理**: 统一版本管理，避免冲突
✅ **构建优化**: 支持独立构建和测试