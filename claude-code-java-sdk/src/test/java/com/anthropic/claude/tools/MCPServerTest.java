package com.anthropic.claude.tools;

import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP服务器测试
 */
class MCPServerTest {

    private MCPServer mcpServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mcpServer = new MCPServer();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (mcpServer != null) {
            mcpServer.shutdown();
        }
    }

    @Test
    void testRegisterTools() {
        // 测试工具注册
        TestTools testTools = new TestTools();
        mcpServer.registerTools(testTools);

        // 验证工具已注册
        assertTrue(mcpServer.hasTool("add_numbers"));
        assertTrue(mcpServer.hasTool("greet"));
        // 注意：TestTools类有3个工具方法（包括slow_task）
        assertEquals(3, mcpServer.getToolNames().size());
    }

    @Test
    void testExecuteTool() throws Exception {
        // 注册测试工具
        TestTools testTools = new TestTools();
        mcpServer.registerTools(testTools);

        // 准备参数
        JsonNode args = objectMapper.readTree("{\"a\": 10, \"b\": 20}");

        // 执行工具
        CompletableFuture<ToolExecutionResult> future = mcpServer.executeTool("add_numbers", args);
        ToolExecutionResult result = future.get();

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(30, result.getResult());
    }

    @Test
    void testExecuteNonExistentTool() throws Exception {
        // 执行不存在的工具
        JsonNode args = objectMapper.readTree("{}");
        CompletableFuture<ToolExecutionResult> future = mcpServer.executeTool("non_existent", args);
        ToolExecutionResult result = future.get();

        // 验证错误结果
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("工具不存在"));
    }

    @Test
    void testUnregisterTool() {
        // 注册工具
        TestTools testTools = new TestTools();
        mcpServer.registerTools(testTools);

        // 注销工具
        assertTrue(mcpServer.unregisterTool("add_numbers"));
        assertFalse(mcpServer.hasTool("add_numbers"));
        assertTrue(mcpServer.hasTool("greet")); // 其他工具仍存在
    }

    @Test
    void testUnregisterInstance() {
        // 注册工具
        TestTools testTools = new TestTools();
        mcpServer.registerTools(testTools);

        // 注销整个实例
        mcpServer.unregisterInstance(testTools);
        assertFalse(mcpServer.hasTool("add_numbers"));
        assertFalse(mcpServer.hasTool("greet"));
        assertEquals(0, mcpServer.getToolNames().size());
    }

    @Test
    void testDuplicateToolName() {
        // 注册第一个工具
        TestTools testTools1 = new TestTools();
        mcpServer.registerTools(testTools1);

        // 尝试注册同名工具应该失败
        TestTools testTools2 = new TestTools();
        assertThrows(ClaudeCodeException.class, () -> {
            mcpServer.registerTools(testTools2);
        });
    }

    @Test
    void testGetStatistics() {
        // 注册工具
        TestTools testTools = new TestTools();
        mcpServer.registerTools(testTools);

        // 获取统计信息
        Map<String, Object> stats = mcpServer.getStatistics();
        assertEquals(3, stats.get("totalTools")); // TestTools有3个工具方法
        assertEquals(1, stats.get("registeredInstances"));
    }

    /**
     * 测试工具类
     */
    static class TestTools {

        @Tool(
                name = "add_numbers",
                description = "Add two numbers together",
                async = false
        )
        public int addNumbers(@com.anthropic.claude.tools.Param("a") int a,
                             @com.anthropic.claude.tools.Param("b") int b) {
            return a + b;
        }

        @Tool(
                name = "greet",
                description = "Greet a person",
                async = false
        )
        public String greet(@com.anthropic.claude.tools.Param("name") String name) {
            return "Hello, " + name + "!";
        }

        @Tool(
                name = "slow_task",
                description = "A slow task for testing timeout",
                async = true,
                timeout = 1000
        )
        public String slowTask() throws InterruptedException {
            Thread.sleep(2000); // 超时测试
            return "Done";
        }
    }
}