package com.anthropic.claude.query;

import com.anthropic.claude.messages.Message;
import io.reactivex.rxjava3.core.Observable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class QueryBuilder {
    private final QueryRequest.Builder requestBuilder;
    private final QueryService queryService;

    public QueryBuilder(String prompt, QueryService queryService) {
        this.requestBuilder = QueryRequest.builder(prompt);
        this.queryService = queryService;
    }

    public QueryBuilder withTools(String... tools) {
        requestBuilder.withTools(tools);
        return this;
    }

    public QueryBuilder withContext(String context) {
        requestBuilder.withContext(context);
        return this;
    }

    public QueryBuilder withMaxTokens(int maxTokens) {
        requestBuilder.withMaxTokens(maxTokens);
        return this;
    }

    public QueryBuilder withTemperature(double temperature) {
        requestBuilder.withTemperature(temperature);
        return this;
    }

    public QueryBuilder withTimeout(Duration timeout) {
        requestBuilder.withTimeout(timeout);
        return this;
    }

    public QueryBuilder addMetadata(String key, Object value) {
        requestBuilder.addMetadata(key, value);
        return this;
    }

    public CompletableFuture<String> execute() {
        return queryService.queryAsync(requestBuilder.build())
                .thenApply(messages -> messages
                        .map(Message::getContent)
                        .reduce("", (a, b) -> a + b));
    }

    public CompletableFuture<Stream<Message>> stream() {
        return queryService.queryAsync(requestBuilder.build());
    }

    public Observable<Message> observe() {
        return queryService.queryStream(requestBuilder.build());
    }
}