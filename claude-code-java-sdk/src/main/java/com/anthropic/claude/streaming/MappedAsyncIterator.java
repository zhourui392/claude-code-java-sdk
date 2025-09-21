package com.anthropic.claude.streaming;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 映射异步迭代器
 * 对源迭代器的每个元素应用映射函数
 *
 * @param <T> 源元素类型
 * @param <R> 结果元素类型
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class MappedAsyncIterator<T, R> implements AsyncIterator<R> {

    private final AsyncIterator<T> source;
    private final Function<T, R> mapper;

    public MappedAsyncIterator(AsyncIterator<T> source, Function<T, R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<Boolean> hasNext() {
        return source.hasNext();
    }

    @Override
    public CompletableFuture<R> next() {
        return source.next().thenApply(mapper);
    }
}