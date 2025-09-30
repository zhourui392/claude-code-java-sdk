package com.anthropic.claude.strategy;

import com.anthropic.claude.config.CliMode;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageParser;
import com.anthropic.claude.process.ProcessManager;
import com.anthropic.claude.query.QueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zeroturnaround.exec.ProcessResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * CLI执行策略测试
 */
class CliExecutionStrategyTest {

    @Mock
    private ProcessManager processManager;

    @Mock
    private MessageParser messageParser;

    @Mock
    private ProcessResult processResult;

    private ClaudeCodeOptions options;
    private QueryRequest queryRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        options = ClaudeCodeOptions.builder()
            .cliMode(CliMode.BATCH)
            .cliPath("claude-code")
            .timeout(Duration.ofMinutes(5))
            .build();

        queryRequest = QueryRequest.builder("测试查询")
            .withMaxTokens(1000)
            .build();
    }

    @Test
    void testBatchProcessStrategy() throws Exception {
        // Arrange
        BatchProcessStrategy strategy = new BatchProcessStrategy(processManager, messageParser, options);

        when(processManager.isCommandAvailable(anyString())).thenReturn(true);
        when(processManager.executeSync(anyList(), any(Duration.class))).thenReturn(processResult);
        when(processResult.getExitValue()).thenReturn(0);
        when(processResult.outputUTF8()).thenReturn("测试输出");

        List<Message> mockMessages = new ArrayList<>();
        when(messageParser.parseMessages(anyString())).thenReturn(mockMessages);

        // Act
        strategy.start();
        Stream<Message> result = strategy.execute(queryRequest);

        // Assert
        assertTrue(strategy.isAvailable());
        assertEquals("BatchProcess", strategy.getStrategyType());
        assertTrue(strategy.supportsSessionPersistence());
        assertNotNull(result);

        verify(processManager).executeSync(anyList(), any(Duration.class));
        verify(messageParser).parseMessages("测试输出");
    }

    @Test
    void testBatchProcessStrategyWithError() throws Exception {
        // Arrange
        BatchProcessStrategy strategy = new BatchProcessStrategy(processManager, messageParser, options);

        when(processManager.isCommandAvailable(anyString())).thenReturn(true);
        when(processManager.executeSync(anyList(), any(Duration.class))).thenReturn(processResult);
        when(processResult.getExitValue()).thenReturn(1);
        when(processResult.outputUTF8()).thenReturn("错误输出");

        // Act & Assert
        strategy.start();
        assertThrows(Exception.class, () -> {
            strategy.execute(queryRequest);
        });
    }

    @Test
    void testBatchProcessStrategyCommandBuilding() throws Exception {
        // Arrange
        ClaudeCodeOptions customOptions = ClaudeCodeOptions.builder()
            .cliMode(CliMode.BATCH)
            .cliPath("claude-code")
            .addAdditionalArg("--verbose")
            .build();

        QueryRequest customRequest = QueryRequest.builder("测试提示")
            .withTools("Read", "Write")
            .withMaxTokens(500)
            .withTemperature(0.7)
            .build();

        BatchProcessStrategy strategy = new BatchProcessStrategy(processManager, messageParser, customOptions);

        when(processManager.isCommandAvailable(anyString())).thenReturn(true);
        when(processManager.executeSync(anyList(), any(Duration.class))).thenReturn(processResult);
        when(processResult.getExitValue()).thenReturn(0);
        when(processResult.outputUTF8()).thenReturn("{}");

        List<Message> mockMessages = new ArrayList<>();
        when(messageParser.parseMessages(anyString())).thenReturn(mockMessages);

        // Act
        strategy.start();
        strategy.execute(customRequest);

        // Assert
        // 验证命令构建包含正确的参数
        verify(processManager).executeSync(argThat(command -> {
            String commandStr = String.join(" ", command);
            return commandStr.contains("claude-code") &&
                   commandStr.contains("测试提示") &&
                   commandStr.contains("--print") &&
                   commandStr.contains("--tool Read") &&
                   commandStr.contains("--tool Write") &&
                   commandStr.contains("--max-tokens 500") &&
                   commandStr.contains("--temperature 0.7") &&
                   commandStr.contains("--verbose");
        }), any(Duration.class));
    }

    @Test
    void testCliModeConfiguration() {
        // Test default CLI mode
        assertEquals(CliMode.BATCH, CliMode.getDefault());

        // Test CLI mode descriptions
        assertEquals("批处理模式", CliMode.BATCH.getDescription());
        assertEquals("PTY交互模式", CliMode.PTY_INTERACTIVE.getDescription());
    }

    @Test
    void testClaudeCodeOptionsWithCliMode() {
        // Test default configuration
        ClaudeCodeOptions defaultOptions = ClaudeCodeOptions.builder().build();
        assertEquals(CliMode.BATCH, defaultOptions.getCliMode());
        assertEquals(Duration.ofSeconds(10), defaultOptions.getPtyReadyTimeout());
        assertNotNull(defaultOptions.getAdditionalArgs());
        assertTrue(defaultOptions.getAdditionalArgs().isEmpty());

        // Test custom configuration
        ClaudeCodeOptions customOptions = ClaudeCodeOptions.builder()
            .cliMode(CliMode.PTY_INTERACTIVE)
            .ptyReadyTimeout(Duration.ofSeconds(15))
            .promptPattern("claude>.*")
            .addAdditionalArg("--verbose")
            .addAdditionalArg("--debug")
            .build();

        assertEquals(CliMode.PTY_INTERACTIVE, customOptions.getCliMode());
        assertEquals(Duration.ofSeconds(15), customOptions.getPtyReadyTimeout());
        assertEquals("claude>.*", customOptions.getPromptPattern());
        assertEquals(2, customOptions.getAdditionalArgs().size());
        assertTrue(customOptions.getAdditionalArgs().contains("--verbose"));
        assertTrue(customOptions.getAdditionalArgs().contains("--debug"));
    }

    @Test
    void testStrategyFactory() throws Exception {
        // Test batch strategy creation
        CliExecutionStrategy batchStrategy = CliExecutionStrategyFactory.createDefaultStrategy(
            processManager, messageParser, options);

        assertNotNull(batchStrategy);
        assertEquals("BatchProcess", batchStrategy.getStrategyType());
        assertTrue(batchStrategy.supportsSessionPersistence());
    }

    @Test
    void testStrategyAvailability() {
        // Test strategy availability when process manager is available
        when(processManager.isCommandAvailable(anyString())).thenReturn(true);

        BatchProcessStrategy strategy = new BatchProcessStrategy(processManager, messageParser, options);
        assertTrue(strategy.isAvailable());

        // Test strategy availability when process manager is not available
        when(processManager.isCommandAvailable(anyString())).thenReturn(false);
        assertFalse(strategy.isAvailable());
    }

    @Test
    void testStrategyShutdown() throws Exception {
        BatchProcessStrategy strategy = new BatchProcessStrategy(processManager, messageParser, options);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            strategy.start();
            strategy.shutdown();
        });
    }
}