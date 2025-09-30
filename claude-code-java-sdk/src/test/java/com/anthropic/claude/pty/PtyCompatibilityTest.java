package com.anthropic.claude.pty;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PTY兼容性测试
 * 确保JDK 8兼容的pty4j依赖正常工作
 */
class PtyCompatibilityTest {

    @Test
    void testPtyManagerCanBeCreated() {
        // 测试PtyManager可以正常创建
        assertDoesNotThrow(() -> {
            PtyManager ptyManager = new PtyManager();
            assertNotNull(ptyManager);
        });
    }

    @Test
    void testPtyManagerState() {
        PtyManager ptyManager = new PtyManager();

        // 初始状态应该是未运行
        assertFalse(ptyManager.isAlive());
        assertEquals("PtyManager", ptyManager.getClass().getSimpleName());
    }
}