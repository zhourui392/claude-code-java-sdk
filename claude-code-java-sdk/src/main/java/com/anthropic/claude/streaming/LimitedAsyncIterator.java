package com.anthropic.claude.streaming;

import java.util.concurrent.CompletableFuture;

/**
 * 限制异步迭代器
 * 限制返回的元素数量
 *
 * @param <T> 元素类型
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class LimitedAsyncIterator<T> implements AsyncIterator<T> {

    private final AsyncIterator<T> source;
    private final long limit;
    private long consumed = 0;

    public LimitedAsyncIterator(AsyncIterator<T> source, long limit) {
        this.source = source;
        this.limit = limit;
    }

    @Override
    public CompletableFuture<Boolean> hasNext() {
        if (consumed >= limit) {
            return CompletableFuture.completedFuture(false);
        }
        return source.hasNext();
    }

    @Override
    public CompletableFuture<T> next() {
        if (consumed >= limit) {
            throw new java.util.NoSuchElementException("已达到限制数量: " + limit);
        }
        consumed++;
        return source.next();
    }
}