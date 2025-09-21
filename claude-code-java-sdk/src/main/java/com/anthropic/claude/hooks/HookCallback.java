package com.anthropic.claude.hooks;

@FunctionalInterface
public interface HookCallback {
    HookResult execute(HookContext context);
}