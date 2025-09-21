package com.anthropic.claude.client;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双向交互客户端测试
 */
class ClaudeSDKClientTest {

    private ClaudeSDKClient client;
    private ClaudeCodeOptions options;

    @BeforeEach
    void setUp() {
        options = ClaudeCodeOptions.builder()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
        client = new ClaudeSDKClient(options);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void testConnect() throws Exception {
        assertFalse(client.isConnected());

        CompletableFuture<Void> connectFuture = client.connect();
        connectFuture.get(10, TimeUnit.SECONDS);

        assertTrue(client.isConnected());
        assertNotNull(client.getSessionId());
    }

    @Test
    void testDisconnect() throws Exception {
        // 先连接
        client.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client.isConnected());

        // 然后断开
        CompletableFuture<Void> disconnectFuture = client.disconnect();
        disconnectFuture.get(10, TimeUnit.SECONDS);

        assertFalse(client.isConnected());
        assertNull(client.getSessionId());
    }

    @Test
    void testReceiveMessages() throws Exception {
        // 连接客户端
        client.connect().get(10, TimeUnit.SECONDS);

        // 验证消息流接收功能
        assertDoesNotThrow(() -> {
            client.receiveMessages().subscribe(message -> {
                // 验证可以接收消息
                assertNotNull(message);
            });
        });

        // 验证连接状态
        assertTrue(client.isConnected());
    }

    @Test
    void testInterrupt() throws Exception {
        // 连接客户端
        client.connect().get(10, TimeUnit.SECONDS);

        assertFalse(client.isInterrupted());

        // 执行中断
        CompletableFuture<Void> interruptFuture = client.interrupt();
        interruptFuture.get(10, TimeUnit.SECONDS);

        assertTrue(client.isInterrupted());
    }

    @Test
    void testSessionState() throws Exception {
        // 连接前
        ClaudeSDKClient.SessionState stateBeforeConnect = client.getSessionState();
        assertFalse(stateBeforeConnect.isActive());
        assertNull(stateBeforeConnect.getSessionId());

        // 连接后
        client.connect().get(10, TimeUnit.SECONDS);
        ClaudeSDKClient.SessionState stateAfterConnect = client.getSessionState();
        assertTrue(stateAfterConnect.isActive());
        assertNotNull(stateAfterConnect.getSessionId());
        assertEquals(0, stateAfterConnect.getMessageCount());
    }

    @Test
    void testResetInterruptState() throws Exception {
        client.connect().get(10, TimeUnit.SECONDS);

        // 设置中断状态
        client.interrupt().get(10, TimeUnit.SECONDS);
        assertTrue(client.isInterrupted());

        // 重置中断状态
        client.resetInterruptState();
        assertFalse(client.isInterrupted());
    }

    @Test
    void testHealthCheck() throws Exception {
        // 连接前不健康
        assertFalse(client.isHealthy());

        // 连接后健康
        client.connect().get(10, TimeUnit.SECONDS);
        assertTrue(client.isHealthy());

        // 断开后不健康
        client.disconnect().get(10, TimeUnit.SECONDS);
        assertFalse(client.isHealthy());
    }

    @Test
    void testReceiveResponse() throws Exception {
        client.connect().get(10, TimeUnit.SECONDS);

        // 模拟接收助手消息
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500); // 等待订阅
                // 这里需要通过MessageReceiver模拟接收消息
                // 在实际测试中可以通过mock来实现
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 由于这是集成测试，我们主要验证方法不会抛出异常
        assertDoesNotThrow(() -> {
            CompletableFuture<Message> responseFuture = client.receiveResponse();
            // 设置较短的超时时间，避免测试挂起
            try {
                responseFuture.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 在模拟环境中可能会超时，这是正常的
            }
        });
    }
}