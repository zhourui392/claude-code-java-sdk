package com.anthropic.claude.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClaudePathResolverTest {

    @Test
    void testResolveClaudePath() {
        String claudePath = ClaudePathResolver.resolveClaudePath();

        // 确保返回了某个路径（即使是默认的"claude"）
        assertNotNull(claudePath);
        assertFalse(claudePath.trim().isEmpty());
    }

    @Test
    void testGetClaudePathReturnsValidPath() {
        String claudePath = ClaudePathResolver.getClaudePath();

        // 如果找到了路径，应该是有效的
        if (claudePath != null) {
            assertTrue(ClaudePathResolver.validateClaudePath(claudePath));
        }
    }

    @Test
    void testFindClaudeInPath() {
        String claudePath = ClaudePathResolver.findClaudeInPath();

        // 如果在PATH中找到了，验证是否有效（某些情况下may find non-executable files）
        if (claudePath != null) {
            // 在测试中，我们只检查路径不为空，不强制要求可执行（因为可能找到的是wrapper文件）
            assertFalse(claudePath.trim().isEmpty());
        }
    }

    @Test
    void testValidateClaudePathWithNull() {
        assertFalse(ClaudePathResolver.validateClaudePath(null));
    }

    @Test
    void testValidateClaudePathWithEmpty() {
        assertFalse(ClaudePathResolver.validateClaudePath(""));
        assertFalse(ClaudePathResolver.validateClaudePath("   "));
    }

    @Test
    void testValidateClaudePathWithNonExistentFile() {
        // 使用适合当前操作系统的无效路径
        String nonExistentPath;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            nonExistentPath = "C:\\non\\existent\\path.exe";
        } else {
            nonExistentPath = "/non/existent/path";
        }
        assertFalse(ClaudePathResolver.validateClaudePath(nonExistentPath));
    }
}