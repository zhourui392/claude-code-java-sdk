package com.anthropic.claude.pty;

import java.util.regex.Pattern;

/**
 * Claude CLI输出模式识别
 *
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class ClaudeOutputPatterns {

    // 提示符模式 - 检测各种命令提示符
    public static final Pattern PROMPT_PATTERN =
        Pattern.compile("^(\\$\\s+|>\\s+|claude>\\s*|\\[.*\\]\\$\\s+|❯\\s+)");

    // 错误模式 - 检测错误信息
    public static final Pattern ERROR_PATTERN =
        Pattern.compile("(?i)(error|failed|exception|不能|失败|错误|无法|invalid|missing).*");

    // 使用限制模式 - 检测API限制
    public static final Pattern USAGE_LIMIT_PATTERN =
        Pattern.compile("(?i)(usage.?limit|rate.?limit|quota.*exceeded|请求过于频繁|达到使用限制|limit exceeded)");

    // 会话恢复模式 - 检测会话相关信息
    public static final Pattern SESSION_RESTORED_PATTERN =
        Pattern.compile("(?i)(session.*restored|会话.*恢复|resuming.*session|continuing.*conversation|恢复对话)");

    // 流式输出开始
    public static final Pattern STREAMING_START_PATTERN =
        Pattern.compile("^(\\{\"type\":\"stream_start\"|开始生成回复|generating.*response)");

    // 流式输出结束
    public static final Pattern STREAMING_END_PATTERN =
        Pattern.compile("^(\\{\"type\":\"stream_end\"|回复生成完成|response.*completed)");

    // JSON响应模式
    public static final Pattern JSON_RESPONSE_PATTERN =
        Pattern.compile("^\\s*\\{.*\\}\\s*$", Pattern.DOTALL);

    // 认证相关模式
    public static final Pattern AUTH_REQUIRED_PATTERN =
        Pattern.compile("(?i)(authentication.*required|需要认证|login.*required|api.*key.*missing|unauthorized)");

    // 处理中模式
    public static final Pattern PROCESSING_PATTERN =
        Pattern.compile("(?i)(processing|处理中|thinking|analyzing|generating|正在|working)");

    // 完成模式
    public static final Pattern COMPLETION_PATTERN =
        Pattern.compile("(?i)(completed|完成|done|finished|结束|success)");

    // 中断模式
    public static final Pattern INTERRUPT_PATTERN =
        Pattern.compile("(?i)(interrupted|中断|cancelled|取消|stopped|停止)");

    // 等待输入模式
    public static final Pattern WAITING_INPUT_PATTERN =
        Pattern.compile("(?i)(waiting.*input|等待输入|please.*enter|enter.*command|输入命令)");

    // 会话ID提取模式
    public static final Pattern SESSION_ID_PATTERN =
        Pattern.compile("session[_\\s]*id[:\\s]*([a-zA-Z0-9\\-_]+)", Pattern.CASE_INSENSITIVE);

    // Claude CLI版本信息
    public static final Pattern VERSION_PATTERN =
        Pattern.compile("claude[_\\s]*(?:code|cli)?[_\\s]*(?:version)?[:\\s]*([0-9]+\\.[0-9]+\\.[0-9]+)", Pattern.CASE_INSENSITIVE);

    // 进度指示器模式
    public static final Pattern PROGRESS_PATTERN =
        Pattern.compile("([0-9]+)%|\\[([=\\s]+)\\]|\\|([/\\-\\\\|]+)\\|");
}