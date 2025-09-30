package com.claude.gui.executor;

/**
 * Claude CLI配置类
 * 用于管理Claude CLI的各种配置参数
 *
 * @author Claude Code GUI Team
 * @version 1.0.0
 */
public class ClaudeCliConfig {

    private String claudeCommand = "claude";
    private String workingDirectory;
    private int connectionTimeoutMs = 30000;
    private int readTimeoutMs = 60000;
    private boolean enableDebugOutput = false;

    /**
     * 默认构造函数
     */
    public ClaudeCliConfig() {
        // 自动检测Claude CLI命令
        detectClaudeCommand();
    }

    /**
     * 自动检测Claude CLI命令路径
     */
    private void detectClaudeCommand() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            // Windows环境下可能的Claude CLI路径
            String[] possibleCommands = {
                System.getProperty("user.home") + "\\AppData\\Roaming\\npm\\claude.cmd",
                "claude.cmd",
                "claude",
                "claude.exe",
                "npx claude",
                "./claude",  // 当前目录下的claude
                "claude-simulator.bat",  // 添加模拟器支持
                System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Claude\\claude.exe",
                System.getProperty("user.dir") + "\\claude-simulator.bat"  // 当前目录的模拟器
            };

            for (String cmd : possibleCommands) {
                if (isCommandAvailable(cmd)) {
                    this.claudeCommand = cmd;
                    System.out.println("检测到Claude CLI: " + cmd);
                    break;
                }
            }
        } else {
            // Unix/Linux/Mac环境
            String[] possibleCommands = {
                "claude",
                "./claude",  // 当前目录下的claude
                "/usr/local/bin/claude",
                "/opt/claude/bin/claude",
                System.getProperty("user.home") + "/.local/bin/claude"
            };

            for (String cmd : possibleCommands) {
                if (isCommandAvailable(cmd)) {
                    this.claudeCommand = cmd;
                    System.out.println("Detected Claude CLI: " + cmd);
                    break;
                }
            }
        }

        if (claudeCommand.equals("claude")) {
            System.out.println("Warning: No Claude CLI found, using default: " + claudeCommand);
        }
    }

    /**
     * 检查命令是否可用
     *
     * @param command 要检查的命令
     * @return 如果命令可用返回true，否则返回false
     */
    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb;

            // 对于批处理文件和cmd文件，使用不同的检查方式
            if (command.endsWith(".bat") || command.endsWith(".cmd")) {
                // 检查文件是否存在
                java.io.File file = new java.io.File(command);
                if (file.exists()) {
                    return true;
                }
                // 检查当前目录下是否存在
                file = new java.io.File(System.getProperty("user.dir"), command);
                return file.exists();
            } else {
                // 对于普通命令，尝试--version参数
                pb = new ProcessBuilder(command, "--version");
                pb.redirectErrorStream(true);

                // 继承父进程的环境变量，这对Windows系统很重要
                pb.environment().putAll(System.getenv());

                Process process = pb.start();
                boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                return finished && process.exitValue() == 0;
            }
        } catch (Exception e) {
            // 如果检测失败，仍然返回true，让实际启动时再处理错误
            System.out.println("Warning: 无法检测命令 " + command + ": " + e.getMessage());
            return true;
        }
    }

    // Getter和Setter方法

    public String getClaudeCommand() {
        return claudeCommand;
    }

    public void setClaudeCommand(String claudeCommand) {
        this.claudeCommand = claudeCommand;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isEnableDebugOutput() {
        return enableDebugOutput;
    }

    public void setEnableDebugOutput(boolean enableDebugOutput) {
        this.enableDebugOutput = enableDebugOutput;
    }

    /**
     * 构建完整的命令行参数
     *
     * @return 命令列表
     */
    public String[] buildCommandArray() {
        // Claude CLI不支持交互模式的外部调用，需要使用--print参数
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            if (claudeCommand.contains("npx")) {
                return new String[]{"cmd", "/c", claudeCommand, "--output-format", "json-stream", "--stream", "--print"};
            } else if (claudeCommand.endsWith(".bat") || claudeCommand.endsWith(".cmd")) {
                // 对于批处理文件和cmd文件，使用cmd /c执行
                return new String[]{"cmd", "/c", "\"" + claudeCommand + "\"", "--output-format", "json-stream", "--stream", "--print"};
            } else {
                return new String[]{claudeCommand, "--output-format", "json-stream", "--stream", "--print"};
            }
        } else {
            return new String[]{claudeCommand, "--output-format", "json-stream", "--stream", "--print"};
        }
    }

    @Override
    public String toString() {
        return "ClaudeCliConfig{" +
                "claudeCommand='" + claudeCommand + '\'' +
                ", workingDirectory='" + workingDirectory + '\'' +
                ", connectionTimeoutMs=" + connectionTimeoutMs +
                ", readTimeoutMs=" + readTimeoutMs +
                ", enableDebugOutput=" + enableDebugOutput +
                '}';
    }
}
