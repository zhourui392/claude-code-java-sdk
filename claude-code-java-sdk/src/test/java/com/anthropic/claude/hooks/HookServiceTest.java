package com.anthropic.claude.hooks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HookServiceTest {

    private HookService hookService;
    private HookCallback testCallback1;
    private HookCallback testCallback2;
    private HookContext testContext;

    @BeforeEach
    void setUp() {
        hookService = new HookService();

        testCallback1 = (context) -> {
            Map<String, Object> data = new HashMap<>();
            data.put("callback1", "executed");
            return HookResult.proceed(data, "callback1 executed");
        };

        testCallback2 = (context) -> {
            Map<String, Object> data = new HashMap<>();
            data.put("callback2", "executed");
            return HookResult.proceed(data, "callback2 executed");
        };

        Map<String, Object> contextData = new HashMap<>();
        contextData.put("testKey", "testValue");
        testContext = new HookContext("testEvent", contextData, "testSession");
    }

    @Test
    void testAddHookSuccess() {
        assertDoesNotThrow(() -> {
            hookService.addHook("test-event", testCallback1);
        });

        assertTrue(hookService.hasHooks("test-event"));
        assertEquals(1, hookService.getHookCount("test-event"));
    }

    @Test
    void testAddHookWithNullEventType() {
        assertThrows(IllegalArgumentException.class, () -> {
            hookService.addHook(null, testCallback1);
        });
    }

    @Test
    void testAddHookWithEmptyEventType() {
        assertThrows(IllegalArgumentException.class, () -> {
            hookService.addHook("", testCallback1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            hookService.addHook("   ", testCallback1);
        });
    }

    @Test
    void testAddHookWithNullCallback() {
        assertThrows(IllegalArgumentException.class, () -> {
            hookService.addHook("test-event", null);
        });
    }

    @Test
    void testAddMultipleHooksForSameEvent() {
        hookService.addHook("test-event", testCallback1);
        hookService.addHook("test-event", testCallback2);

        assertEquals(2, hookService.getHookCount("test-event"));
    }

    @Test
    void testRemoveHookSuccess() {
        hookService.addHook("test-event", testCallback1);
        hookService.addHook("test-event", testCallback2);

        assertTrue(hookService.removeHook("test-event", testCallback1));
        assertEquals(1, hookService.getHookCount("test-event"));
        assertTrue(hookService.hasHooks("test-event"));
    }

    @Test
    void testRemoveLastHook() {
        hookService.addHook("test-event", testCallback1);

        assertTrue(hookService.removeHook("test-event", testCallback1));
        assertFalse(hookService.hasHooks("test-event"));
        assertEquals(0, hookService.getHookCount("test-event"));
    }

    @Test
    void testRemoveNonExistentHook() {
        assertFalse(hookService.removeHook("test-event", testCallback1));
    }

    @Test
    void testRemoveHookWithNullParameters() {
        assertFalse(hookService.removeHook(null, testCallback1));
        assertFalse(hookService.removeHook("test-event", null));
        assertFalse(hookService.removeHook(null, null));
    }

    @Test
    void testRemoveAllHooks() {
        hookService.addHook("test-event", testCallback1);
        hookService.addHook("test-event", testCallback2);

        hookService.removeAllHooks("test-event");

        assertFalse(hookService.hasHooks("test-event"));
        assertEquals(0, hookService.getHookCount("test-event"));
    }

    @Test
    void testRemoveAllHooksWithNullEventType() {
        hookService.addHook("test-event", testCallback1);

        assertDoesNotThrow(() -> {
            hookService.removeAllHooks(null);
        });

        assertTrue(hookService.hasHooks("test-event"));
    }

    @Test
    void testClearAllHooks() {
        hookService.addHook("event1", testCallback1);
        hookService.addHook("event2", testCallback2);

        hookService.clearAllHooks();

        assertEquals(0, hookService.getTotalHookCount());
        assertFalse(hookService.hasHooks("event1"));
        assertFalse(hookService.hasHooks("event2"));
    }

    @Test
    void testExecuteHooksSuccess() {
        hookService.addHook("test-event", testCallback1);
        hookService.addHook("test-event", testCallback2);

        HookResult result = hookService.executeHooks("test-event", testContext);

        assertTrue(result.shouldContinue());
        assertEquals("callback1 executed; callback2 executed", result.getMessage());
        assertTrue(result.getModifiedData().containsKey("callback1"));
        assertTrue(result.getModifiedData().containsKey("callback2"));
    }

    @Test
    void testExecuteHooksWithStopCallback() {
        HookCallback stopCallback = (context) -> HookResult.stop("stopped by callback");

        hookService.addHook("test-event", testCallback1);
        hookService.addHook("test-event", stopCallback);
        hookService.addHook("test-event", testCallback2);

        HookResult result = hookService.executeHooks("test-event", testContext);

        assertFalse(result.shouldContinue());
        assertEquals("stopped by callback", result.getMessage());
    }

    @Test
    void testExecuteHooksWithException() {
        HookCallback errorCallback = (context) -> {
            throw new RuntimeException("Test exception");
        };

        hookService.addHook("test-event", testCallback1);
        hookService.addHook("test-event", errorCallback);
        hookService.addHook("test-event", testCallback2);

        HookResult result = hookService.executeHooks("test-event", testContext);

        assertFalse(result.shouldContinue());
        assertTrue(result.getMessage().contains("Hook回调执行失败"));
    }

    @Test
    void testExecuteHooksWithNullParameters() {
        HookResult result1 = hookService.executeHooks(null, testContext);
        assertTrue(result1.shouldContinue());

        HookResult result2 = hookService.executeHooks("test-event", null);
        assertTrue(result2.shouldContinue());
    }

    @Test
    void testExecuteHooksWithNoRegisteredHooks() {
        HookResult result = hookService.executeHooks("non-existent-event", testContext);

        assertTrue(result.shouldContinue());
        assertNull(result.getMessage());
    }

    @Test
    void testHasHooks() {
        assertFalse(hookService.hasHooks("test-event"));

        hookService.addHook("test-event", testCallback1);
        assertTrue(hookService.hasHooks("test-event"));

        hookService.removeHook("test-event", testCallback1);
        assertFalse(hookService.hasHooks("test-event"));
    }

    @Test
    void testGetHookCount() {
        assertEquals(0, hookService.getHookCount("test-event"));

        hookService.addHook("test-event", testCallback1);
        assertEquals(1, hookService.getHookCount("test-event"));

        hookService.addHook("test-event", testCallback2);
        assertEquals(2, hookService.getHookCount("test-event"));

        hookService.removeHook("test-event", testCallback1);
        assertEquals(1, hookService.getHookCount("test-event"));
    }

    @Test
    void testGetRegisteredEventTypes() {
        assertTrue(hookService.getRegisteredEventTypes().isEmpty());

        hookService.addHook("event1", testCallback1);
        hookService.addHook("event2", testCallback2);

        List<String> eventTypes = hookService.getRegisteredEventTypes();
        assertEquals(2, eventTypes.size());
        assertTrue(eventTypes.contains("event1"));
        assertTrue(eventTypes.contains("event2"));
    }

    @Test
    void testGetTotalHookCount() {
        assertEquals(0, hookService.getTotalHookCount());

        hookService.addHook("event1", testCallback1);
        hookService.addHook("event2", testCallback2);
        hookService.addHook("event1", testCallback2);

        assertEquals(3, hookService.getTotalHookCount());
    }

    @Test
    void testGetHookStatistics() {
        Map<String, Integer> stats = hookService.getHookStatistics();
        assertTrue(stats.isEmpty());

        hookService.addHook("event1", testCallback1);
        hookService.addHook("event1", testCallback2);
        hookService.addHook("event2", testCallback1);

        stats = hookService.getHookStatistics();
        assertEquals(2, stats.size());
        assertEquals(2, stats.get("event1"));
        assertEquals(1, stats.get("event2"));
    }

    @Test
    void testConcurrentAccess() {
        hookService.addHook("test-event", testCallback1);

        assertDoesNotThrow(() -> {
            Thread thread1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    hookService.executeHooks("test-event", testContext);
                }
            });

            Thread thread2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    hookService.addHook("test-event-" + i, testCallback2);
                }
            });

            thread1.start();
            thread2.start();

            thread1.join();
            thread2.join();
        });
    }
}