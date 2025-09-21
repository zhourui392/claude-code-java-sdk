package com.anthropic.claude.process;

import com.anthropic.claude.exceptions.ProcessExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessManagerTest {

    private ProcessManager processManager;

    @BeforeEach
    void setUp() {
        Map<String, String> environment = new HashMap<>();
        environment.put("TEST_VAR", "test-value");
        processManager = new ProcessManager(Duration.ofSeconds(10), environment);
    }

    @Test
    void testConstructorWithDefaults() {
        ProcessManager defaultManager = new ProcessManager();
        assertNotNull(defaultManager);
    }

    @Test
    void testConstructorWithParameters() {
        Duration timeout = Duration.ofMinutes(5);
        Map<String, String> env = Map.of("KEY", "value");

        ProcessManager manager = new ProcessManager(timeout, env);
        assertNotNull(manager);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecuteSyncSuccessOnWindows() throws ProcessExecutionException {
        List<String> command = Arrays.asList("cmd", "/c", "echo", "Hello World");

        assertDoesNotThrow(() -> {
            var result = processManager.executeSync(command);
            assertNotNull(result);
            assertEquals(0, result.getExitValue());
            assertTrue(result.outputUTF8().contains("Hello World"));
        });
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExecuteSyncSuccessOnUnix() throws ProcessExecutionException {
        List<String> command = Arrays.asList("echo", "Hello World");

        assertDoesNotThrow(() -> {
            var result = processManager.executeSync(command);
            assertNotNull(result);
            assertEquals(0, result.getExitValue());
            assertTrue(result.outputUTF8().contains("Hello World"));
        });
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecuteSyncWithTimeoutOnWindows() {
        List<String> command = Arrays.asList("cmd", "/c", "echo", "test");
        Duration shortTimeout = Duration.ofSeconds(1);

        assertDoesNotThrow(() -> {
            var result = processManager.executeSync(command, shortTimeout);
            assertNotNull(result);
        });
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExecuteSyncWithTimeoutOnUnix() {
        List<String> command = Arrays.asList("echo", "test");
        Duration shortTimeout = Duration.ofSeconds(1);

        assertDoesNotThrow(() -> {
            var result = processManager.executeSync(command, shortTimeout);
            assertNotNull(result);
        });
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecuteSyncFailureOnWindows() {
        List<String> command = Arrays.asList("cmd", "/c", "exit", "1");

        assertThrows(ProcessExecutionException.class, () -> {
            processManager.executeSync(command);
        });
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExecuteSyncFailureOnUnix() {
        List<String> command = Arrays.asList("sh", "-c", "exit 1");

        assertThrows(ProcessExecutionException.class, () -> {
            processManager.executeSync(command);
        });
    }

    @Test
    void testExecuteSyncWithInvalidCommand() {
        List<String> command = Arrays.asList("non-existent-command");

        assertThrows(ProcessExecutionException.class, () -> {
            processManager.executeSync(command);
        });
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsCommandAvailableWithValidCommandOnWindows() {
        assertTrue(processManager.isCommandAvailable("cmd"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testIsCommandAvailableWithValidCommandOnUnix() {
        assertTrue(processManager.isCommandAvailable("echo"));
    }

    @Test
    void testIsCommandAvailableWithInvalidCommand() {
        assertFalse(processManager.isCommandAvailable("non-existent-command-12345"));
    }

    @Test
    void testIsCommandAvailableWithNullCommand() {
        assertFalse(processManager.isCommandAvailable(null));
    }

    @Test
    void testIsCommandAvailableWithEmptyCommand() {
        assertFalse(processManager.isCommandAvailable(""));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecuteAsyncOnWindows() {
        List<String> command = Arrays.asList("cmd", "/c", "echo", "Async Test");

        assertDoesNotThrow(() -> {
            var future = processManager.executeAsync(command);
            assertNotNull(future);
            var result = future.get();
            assertNotNull(result);
        });
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExecuteAsyncOnUnix() {
        List<String> command = Arrays.asList("echo", "Async Test");

        assertDoesNotThrow(() -> {
            var future = processManager.executeAsync(command);
            assertNotNull(future);
            var result = future.get();
            assertNotNull(result);
        });
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecuteStreamingOnWindows() {
        List<String> command = Arrays.asList("cmd", "/c", "echo", "Streaming Test");
        StringBuilder output = new StringBuilder();

        assertDoesNotThrow(() -> {
            processManager.executeStreaming(command, line -> {
                output.append(line).append("\n");
            });
        });
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExecuteStreamingOnUnix() {
        List<String> command = Arrays.asList("echo", "Streaming Test");
        StringBuilder output = new StringBuilder();

        assertDoesNotThrow(() -> {
            processManager.executeStreaming(command, line -> {
                output.append(line).append("\n");
            });
        });
    }

    @Test
    void testExecuteAsyncWithInvalidCommand() {
        List<String> command = Arrays.asList("non-existent-command");

        var future = processManager.executeAsync(command);
        assertNotNull(future);

        assertThrows(Exception.class, () -> {
            future.get();
        });
    }
}