# Claude Code CLI GUI 部署和运行指南

## 系统要求

### 运行环境
- **操作系统**: Windows 10/11, macOS 10.12+, Ubuntu 18.04+
- **Java版本**: JDK 1.8 或更高版本
- **内存**: 最少 256MB RAM
- **Claude Code CLI**: 需要预先安装

### 安装依赖

#### 1. 安装Java
```bash
# Windows - 下载并安装Oracle JDK或OpenJDK
# 从 https://www.oracle.com/java/technologies/downloads/ 下载

# macOS - 使用Homebrew
brew install openjdk@8

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-8-jdk

# 验证安装
java -version
```

#### 2. 安装Claude Code CLI
```bash
# 请参考Claude Code官方文档安装CLI工具
# 确保claude命令在系统PATH中可用
claude --version
```

## 部署步骤

### 1. 下载程序文件
将以下文件复制到目标目录：
- `claude-code-gui-1.0.0-jar-with-dependencies.jar` - 主程序文件
- `start.bat` (Windows) - 启动脚本

### 2. 设置执行权限 (Linux/macOS)
```bash
chmod +x claude-code-gui-1.0.0-jar-with-dependencies.jar
```

### 3. 创建桌面快捷方式 (可选)

#### Windows
1. 右键点击桌面 → 新建 → 快捷方式
2. 输入目标位置：`java -jar "完整路径\claude-code-gui-1.0.0-jar-with-dependencies.jar"`
3. 命名快捷方式为"Claude Code GUI"

#### macOS
创建启动脚本：
```bash
#!/bin/bash
cd "$(dirname "$0")"
java -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

#### Linux
创建.desktop文件：
```ini
[Desktop Entry]
Name=Claude Code GUI
Comment=Claude Code CLI GUI Interface
Exec=java -jar /path/to/claude-code-gui-1.0.0-jar-with-dependencies.jar
Icon=application-x-java
Terminal=false
Type=Application
Categories=Development;
```

## 运行程序

### Windows
1. **方法一**: 双击 `start.bat` 文件
2. **方法二**: 命令行运行
   ```cmd
   java -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
   ```

### macOS/Linux
```bash
java -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

## 故障排除

### 常见问题及解决方案

#### 1. "java: command not found"
**原因**: 未安装Java或PATH配置错误
**解决方案**:
- 安装JDK 1.8或更高版本
- 确保JAVA_HOME环境变量设置正确
- 将Java bin目录添加到PATH

#### 2. "无法连接Claude CLI"
**原因**: Claude CLI未安装或不在PATH中
**解决方案**:
- 安装Claude Code CLI
- 确认`claude --version`命令能正常执行
- 检查网络连接

#### 3. "程序启动后界面空白"
**原因**: 字体或主题问题
**解决方案**:
- 更新系统字体包
- 尝试使用不同的Look and Feel
- 检查系统支持的中文字体

#### 4. "内存不足"错误
**解决方案**:
```bash
# 增加JVM内存限制
java -Xmx512m -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

#### 5. 中文显示乱码
**解决方案**:
```bash
# 设置字符编码
java -Dfile.encoding=UTF-8 -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

### 日志分析
程序运行时会在控制台输出日志信息，如遇问题请查看：
- 启动日志
- 连接日志
- 错误堆栈信息

### 性能优化

#### JVM参数调优
```bash
# 推荐的JVM参数
java -Xms128m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8 \
     -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

#### 环境变量设置
```bash
# Windows
set JAVA_OPTS=-Xms128m -Xmx512m -Dfile.encoding=UTF-8
java %JAVA_OPTS% -jar claude-code-gui-1.0.0-jar-with-dependencies.jar

# Linux/macOS
export JAVA_OPTS="-Xms128m -Xmx512m -Dfile.encoding=UTF-8"
java $JAVA_OPTS -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

## 配置说明

### 自定义Claude CLI路径
如果Claude CLI不在标准PATH中，程序会自动尝试检测以下位置：

**Windows**:
- `claude.exe`
- `%USERPROFILE%\AppData\Local\Programs\Claude\claude.exe`
- `npx claude`

**macOS/Linux**:
- `/usr/local/bin/claude`
- `/opt/claude/bin/claude`
- `~/.local/bin/claude`

### 工作目录设置
程序默认使用当前用户目录作为工作目录，可通过以下方式修改：
```bash
java -Duser.dir=/path/to/workdir -jar claude-code-gui-1.0.0-jar-with-dependencies.jar
```

## 更新和维护

### 版本检查
程序标题栏显示当前版本号，也可通过"关于"菜单查看详细版本信息。

### 备份配置
目前程序不保存配置文件，所有设置在重启后会重置为默认值。

### 卸载程序
删除以下文件即可完全卸载：
- JAR程序文件
- 启动脚本
- 桌面快捷方式

## 技术支持

如遇到问题，请提供以下信息：
1. 操作系统版本
2. Java版本 (`java -version`)
3. Claude CLI版本 (`claude --version`)
4. 错误信息截图
5. 控制台日志输出

---

*最后更新: 2025-09-20*
*版本: 1.0.0*