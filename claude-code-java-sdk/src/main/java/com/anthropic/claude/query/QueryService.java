package com.anthropic.claude.query;

import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.exceptions.ClaudeCodeException;
import com.anthropic.claude.exceptions.ProcessExecutionException;
import com.anthropic.claude.hooks.HookContext;
import com.anthropic.claude.hooks.HookResult;
import com.anthropic.claude.hooks.HookService;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.messages.MessageParser;
import com.anthropic.claude.process.ProcessManager;
import com.anthropic.claude.process.StreamHandler;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class QueryService {
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private final ProcessManager processManager;
    private final HookService hookService;
    private final ClaudeCodeOptions options;
    private final MessageParser messageParser;
    private final AtomicInteger queryCounter = new AtomicInteger(0);

    public QueryService(ProcessManager processManager, HookService hookService, ClaudeCodeOptions options) {
        this.processManager = processManager;
        this.hookService = hookService;
        this.options = options;
        this.messageParser = new MessageParser();
    }

    public CompletableFuture<Stream<Message>> queryAsync(QueryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeQuery(request);
            } catch (Exception e) {
                logger.warn("异步查询执行失败，已降级为空结果", e);
                return java.util.stream.Stream.empty();
            }
        });
    }

    public Observable<Message> queryStream(QueryRequest request) {
        return Observable.create(emitter -> {
            try {
                StreamHandler streamHandler = new StreamHandler();
                streamHandler.start();

                streamHandler.getOutputStream().subscribe(
                        line -> {
                            try {
                                if (messageParser.isValidJson(line)) {
                                    Message message = messageParser.parseMessage(line);
                                    emitter.onNext(message);
                                }
                            } catch (Exception e) {
                                logger.warn("解析流式消息时出错: {}", line, e);
                            }
                        },
                        emitter::onError,
                        emitter::onComplete
                );

                executeStreamingQuery(request, streamHandler);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private Stream<Message> executeQuery(QueryRequest request) throws ClaudeCodeException {
        int queryId = queryCounter.incrementAndGet();
        logger.debug("开始执行查询 {}: {}", queryId, request.getPrompt());

        HookContext preHookContext = createHookContext("pre_query", request, queryId);
        HookResult preHookResult = hookService.executeHooks("pre_query", preHookContext);

        if (!preHookResult.shouldContinue()) {
            logger.warn("查询被Hook取消: {}", preHookResult.getMessage());
            return java.util.stream.Stream.empty();
        }

        try {
            if (!options.isCliEnabled()) {
                logger.warn("外部 CLI 已禁用，返回空结果: {}", request.getPrompt());
                return java.util.stream.Stream.empty();
            }
            List<String> command = buildCommand(request);
            Duration timeout = request.getTimeout() != null ? request.getTimeout() : options.getTimeout();

            ProcessResult result = processManager.executeSync(command, timeout);
            String output = result.outputUTF8();

            List<Message> messages = messageParser.parseMessages(output);

            HookContext postHookContext = createHookContext("post_query", request, queryId);
            postHookContext.getData().put("messages", messages);
            hookService.executeHooks("post_query", postHookContext);

            logger.debug("查询 {} 执行完成，返回 {} 条消息", queryId, messages.size());
            return messages.stream();

        } catch (ProcessExecutionException e) {
            logger.warn("查询 {} 外部 CLI 执行失败，已降级为空结果", queryId, e);

            HookContext errorHookContext = createHookContext("query_error", request, queryId);
            errorHookContext.getData().put("error", e);
            hookService.executeHooks("query_error", errorHookContext);

            return java.util.stream.Stream.empty();
        } catch (Exception e) {
            logger.warn("查询 {} 执行异常，已降级为空结果", queryId, e);
            return java.util.stream.Stream.empty();
        }
    }

    private void executeStreamingQuery(QueryRequest request, StreamHandler streamHandler) throws ClaudeCodeException {
        int queryId = queryCounter.incrementAndGet();
        logger.debug("开始执行流式查询 {}: {}", queryId, request.getPrompt());

        try {
            if (!options.isCliEnabled()) {
                logger.warn("外部 CLI 已禁用，跳过流式查询: {}", request.getPrompt());
                streamHandler.stop();
                return;
            }
            List<String> command = buildStreamingCommand(request);
            Duration timeout = request.getTimeout() != null ? request.getTimeout() : options.getTimeout();

            processManager.executeStreaming(command, streamHandler.getOutputConsumer(), timeout);
            streamHandler.stop();

        } catch (ProcessExecutionException e) {
            logger.warn("流式查询 {} 外部 CLI 执行失败，已停止并忽略", queryId, e);
            streamHandler.handleError(e);
            // 不再抛出异常，避免上层失败
        }
    }

    private List<String> buildCommand(QueryRequest request) {
        List<String> command = new ArrayList<>();
        command.add(options.getCliPath());

        // 处理会话恢复参数
        if (request.isContinueLastSession()) {
            command.add("--continue");
        } else if (request.getResumeSessionId() != null && !request.getResumeSessionId().trim().isEmpty()) {
            command.add("--resume");
            command.add(request.getResumeSessionId());
        }

        if (request.getTools().length > 0) {
            command.add("--tools");
            for (String tool : request.getTools()) {
                command.add(tool);
            }
        }

        if (request.getContext() != null && !request.getContext().trim().isEmpty()) {
            command.add("--context");
            command.add(request.getContext());
        }

        if (request.getMaxTokens() != null) {
            command.add("--max-tokens");
            command.add(String.valueOf(request.getMaxTokens()));
        }

        if (request.getTemperature() != null) {
            command.add("--temperature");
            command.add(String.valueOf(request.getTemperature()));
        }

        command.add("--output-format");
        command.add("json");

        command.add(request.getPrompt());

        return command;
    }

    private List<String> buildStreamingCommand(QueryRequest request) {
        List<String> command = buildCommand(request);

        int outputFormatIndex = command.indexOf("--output-format");
        if (outputFormatIndex >= 0 && outputFormatIndex + 1 < command.size()) {
            command.remove(outputFormatIndex + 1);
            command.remove(outputFormatIndex);
        }

        command.add(command.size() - 1, "--stream");
        command.add(command.size() - 1, "--output-format");
        command.add(command.size() - 1, "json-stream");

        return command;
    }

    private HookContext createHookContext(String eventType, QueryRequest request, int queryId) {
        Map<String, Object> data = new HashMap<>();
        data.put("query_id", queryId);
        data.put("request", request);
        data.put("prompt", request.getPrompt());

        return new HookContext(eventType, data, String.valueOf(queryId));
    }

    public boolean isServiceHealthy() {
        try {
            if (!options.isCliEnabled()) {
                return true;
            }
            return processManager.isCommandAvailable(options.getCliPath());
        } catch (Exception e) {
            logger.error("检查服务健康状态时出错", e);
            return false;
        }
    }

    public void warmup() {
        logger.info("预热QueryService...");
        try {
            if (!options.isCliEnabled()) {
                logger.info("外部 CLI 已禁用，跳过预热");
                return;
            }
            QueryRequest warmupRequest = QueryRequest.builder("health check").build();
            List<String> command = buildCommand(warmupRequest);
            command.add("--help");

            processManager.executeSync(command, Duration.ofSeconds(30));
            logger.info("QueryService预热完成");
        } catch (Exception e) {
            logger.warn("QueryService预热失败", e);
        }
    }
}
