package com.anthropic.claude.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Claude Code路径解析工具类
 * 根据不同操作系统动态获取Claude Code CLI的安装路径
 */
public class ClaudePathResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudePathResolver.class);

    private static final String CLAUDE_EXECUTABLE_NAME = "claude";
    private static final String CLAUDE_CMD_NAME = "claude.cmd";
    private static final String CLAUDE_NPM_NAME = "@anthropic-ai/claude-code";

    /**
     * 获取Claude Code CLI的路径
     *
     * @return Claude CLI的完整路径，如果找不到则返回null
     */
    public static String getClaudePath() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getWindowsClaudePath();
        } else if (os.contains("mac") || os.contains("darwin")) {
            return getMacClaudePath();
        } else if (os.contains("linux") || os.contains("unix")) {
            return getLinuxClaudePath();
        }

        LOGGER.warn("未支持的操作系统: {}", os);
        return null;
    }

    /**
     * Windows系统中查找Claude路径
     */
    private static String getWindowsClaudePath() {
        List<String> candidatePaths = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        String userName = System.getProperty("user.name");

        // NPM全局安装路径
        candidatePaths.add(userHome + "\\AppData\\Roaming\\npm\\" + CLAUDE_CMD_NAME);
        candidatePaths.add("C:\\Users\\" + userName + "\\AppData\\Roaming\\npm\\" + CLAUDE_CMD_NAME);

        // 原生安装路径
        candidatePaths.add(userHome + "\\AppData\\Local\\Programs\\claude\\" + CLAUDE_CMD_NAME);
        candidatePaths.add("C:\\Program Files\\claude\\" + CLAUDE_CMD_NAME);
        candidatePaths.add("C:\\Program Files (x86)\\claude\\" + CLAUDE_CMD_NAME);

        // WSL路径（如果在WSL中运行）
        candidatePaths.add("/usr/local/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add(userHome + "/.local/bin/" + CLAUDE_EXECUTABLE_NAME);

        return findFirstValidPath(candidatePaths);
    }

    /**
     * macOS系统中查找Claude路径
     */
    private static String getMacClaudePath() {
        List<String> candidatePaths = new ArrayList<>();
        String userHome = System.getProperty("user.home");

        // NPM全局安装路径
        candidatePaths.add("/usr/local/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add(userHome + "/.npm-global/bin/" + CLAUDE_EXECUTABLE_NAME);

        // 原生安装路径
        candidatePaths.add(userHome + "/.local/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add("/Applications/Claude.app/Contents/MacOS/" + CLAUDE_EXECUTABLE_NAME);

        // Homebrew路径
        candidatePaths.add("/opt/homebrew/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add("/usr/local/Cellar/claude/*/bin/" + CLAUDE_EXECUTABLE_NAME);

        return findFirstValidPath(candidatePaths);
    }

    /**
     * Linux系统中查找Claude路径
     */
    private static String getLinuxClaudePath() {
        List<String> candidatePaths = new ArrayList<>();
        String userHome = System.getProperty("user.home");

        // NPM全局安装路径
        candidatePaths.add("/usr/local/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add(userHome + "/.npm-global/bin/" + CLAUDE_EXECUTABLE_NAME);

        // 原生安装路径
        candidatePaths.add(userHome + "/.local/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add("/usr/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add("/bin/" + CLAUDE_EXECUTABLE_NAME);

        // Snap安装路径
        candidatePaths.add("/snap/bin/" + CLAUDE_EXECUTABLE_NAME);
        candidatePaths.add(userHome + "/snap/claude/current/bin/" + CLAUDE_EXECUTABLE_NAME);

        // Flatpak路径
        candidatePaths.add(userHome + "/.local/share/flatpak/exports/bin/" + CLAUDE_EXECUTABLE_NAME);

        return findFirstValidPath(candidatePaths);
    }

    /**
     * 从候选路径列表中找到第一个有效的路径
     */
    private static String findFirstValidPath(List<String> candidatePaths) {
        for (String path : candidatePaths) {
            if (isValidExecutablePath(path)) {
                LOGGER.debug("找到Claude CLI路径: {}", path);
                return path;
            }
        }

        LOGGER.warn("未找到Claude CLI，候选路径: {}", candidatePaths);
        return null;
    }

    /**
     * 检查路径是否为有效的可执行文件
     */
    private static boolean isValidExecutablePath(String pathString) {
        if (pathString == null || pathString.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(pathString);
            File file = path.toFile();
            return file.exists() && file.isFile() && file.canExecute();
        } catch (Exception e) {
            LOGGER.trace("检查路径时出错: {}", pathString, e);
            return false;
        }
    }

    /**
     * 使用which/where命令查找Claude
     */
    public static String findClaudeInPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String command = os.contains("win") ? "where" : "which";

        try {
            ProcessBuilder pb = new ProcessBuilder(command, CLAUDE_EXECUTABLE_NAME);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                byte[] output = process.getInputStream().readAllBytes();
                String result = new String(output).trim();
                String[] lines = result.split("\n");

                for (String line : lines) {
                    String foundPath = line.trim();
                    if (!foundPath.isEmpty()) {
                        // 在Windows中优先选择.cmd文件
                        if (os.contains("win") && foundPath.endsWith(".cmd")) {
                            LOGGER.debug("通过{}命令找到Claude: {}", command, foundPath);
                            return foundPath;
                        } else if (!os.contains("win")) {
                            LOGGER.debug("通过{}命令找到Claude: {}", command, foundPath);
                            return foundPath;
                        }
                    }
                }

                // 如果没有找到.cmd文件，返回第一个有效路径
                if (lines.length > 0 && !lines[0].isEmpty()) {
                    String foundPath = lines[0].trim();
                    LOGGER.debug("通过{}命令找到Claude: {}", command, foundPath);
                    return foundPath;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("使用{}命令查找Claude失败", command, e);
        }

        return null;
    }

    /**
     * 获取Claude路径的主方法，结合多种查找策略
     */
    public static String resolveClaudePath() {
        // 首先尝试平台特定的路径查找
        String path = getClaudePath();
        if (path != null) {
            return path;
        }

        // 如果平台特定查找失败，尝试在PATH中查找
        path = findClaudeInPath();
        if (path != null) {
            return path;
        }

        // 都失败了，返回默认的claude命令（让系统PATH处理）
        LOGGER.warn("无法找到Claude CLI的具体路径，将使用默认命令名称");
        return CLAUDE_EXECUTABLE_NAME;
    }

    /**
     * 验证指定路径的Claude是否可用
     */
    public static boolean validateClaudePath(String claudePath) {
        if (claudePath == null || claudePath.trim().isEmpty()) {
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(claudePath, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                LOGGER.debug("Claude路径验证成功: {}", claudePath);
                return true;
            } else {
                LOGGER.debug("Claude路径验证失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            LOGGER.debug("验证Claude路径时出错: {}", claudePath, e);
            return false;
        }
    }
}