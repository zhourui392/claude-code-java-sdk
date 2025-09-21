package com.anthropic.claude.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工具系统工厂类
 * 提供创建MCP服务器和其他工具组件的便捷方法
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ToolsFactory {
    private static final Logger logger = LoggerFactory.getLogger(ToolsFactory.class);

    /**
     * 创建SDK内置的MCP服务器实例
     *
     * @return 新的MCP服务器实例
     */
    public static MCPServer createSDKMCPServer() {
        logger.info("创建SDK MCP服务器");
        MCPServer server = new MCPServer();

        // 注册内置工具（如果有的话）
        registerBuiltinTools(server);

        return server;
    }

    /**
     * 创建带有预配置工具的MCP服务器
     *
     * @param toolInstances 要注册的工具实例
     * @return 配置好的MCP服务器实例
     */
    public static MCPServer createSDKMCPServer(Object... toolInstances) {
        MCPServer server = createSDKMCPServer();

        for (Object instance : toolInstances) {
            if (instance != null) {
                server.registerTools(instance);
            }
        }

        return server;
    }

    /**
     * 创建工具执行器
     *
     * @return 新的工具执行器实例
     */
    public static ToolExecutor createToolExecutor() {
        logger.debug("创建工具执行器");
        return new ToolExecutor();
    }

    /**
     * 注册内置工具
     */
    private static void registerBuiltinTools(MCPServer server) {
        // 这里可以注册SDK内置的工具
        // 例如：文件操作、系统信息等通用工具
        logger.debug("注册内置工具");

        // 示例：注册系统信息工具
        server.registerTools(new SystemInfoTools());
    }

    /**
     * 内置系统信息工具
     */
    public static class SystemInfoTools {

        @Tool(
                name = "get_system_info",
                description = "获取系统基本信息",
                async = false
        )
        public SystemInfo getSystemInfo() {
            return new SystemInfo(
                    System.getProperty("os.name"),
                    System.getProperty("os.version"),
                    System.getProperty("java.version"),
                    Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().totalMemory(),
                    Runtime.getRuntime().freeMemory()
            );
        }

        @Tool(
                name = "get_current_time",
                description = "获取当前时间戳",
                async = false
        )
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }

        @Tool(
                name = "gc",
                description = "执行垃圾回收",
                async = true
        )
        public String performGC() {
            long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.gc();
            long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long freed = beforeMemory - afterMemory;
            return String.format("GC执行完成，释放内存: %d bytes", freed);
        }

        /**
         * 系统信息数据类
         */
        public static class SystemInfo {
            private final String osName;
            private final String osVersion;
            private final String javaVersion;
            private final int processors;
            private final long totalMemory;
            private final long freeMemory;

            public SystemInfo(String osName, String osVersion, String javaVersion,
                            int processors, long totalMemory, long freeMemory) {
                this.osName = osName;
                this.osVersion = osVersion;
                this.javaVersion = javaVersion;
                this.processors = processors;
                this.totalMemory = totalMemory;
                this.freeMemory = freeMemory;
            }

            public String getOsName() { return osName; }
            public String getOsVersion() { return osVersion; }
            public String getJavaVersion() { return javaVersion; }
            public int getProcessors() { return processors; }
            public long getTotalMemory() { return totalMemory; }
            public long getFreeMemory() { return freeMemory; }
            public long getUsedMemory() { return totalMemory - freeMemory; }

            @Override
            public String toString() {
                return String.format("SystemInfo{os=%s %s, java=%s, cpu=%d cores, memory=%d/%d MB}",
                        osName, osVersion, javaVersion, processors,
                        getUsedMemory() / 1024 / 1024, totalMemory / 1024 / 1024);
            }
        }
    }
}