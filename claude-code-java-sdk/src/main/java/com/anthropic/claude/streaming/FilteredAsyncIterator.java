package com.anthropic.claude.streaming;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * 过滤异步迭代器
 * 只返回满足条件的元素
 *
 * @param <T> 元素类型
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class FilteredAsyncIterator<T> implements AsyncIterator<T> {

    private final AsyncIterator<T> source;
    private final Predicate<T> predicate;
    private CompletableFuture<T> nextItem = null;

    public FilteredAsyncIterator(AsyncIterator<T> source, Predicate<T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public CompletableFuture<Boolean> hasNext() {
        if (nextItem != null) {
            return CompletableFuture.completedFuture(true);
        }

        return findNext().thenApply(found -> found != null);
    }

    @Override
    public CompletableFuture<T> next() {
        if (nextItem != null) {
            CompletableFuture<T> result = nextItem;
            nextItem = null;
            return result;
        }

        return findNext().thenCompose(found -> {
            if (found != null) {
                return found;
            } else {
                throw new java.util.NoSuchElementException("没有更多满足条件的元素");
            }
        });
    }

    private CompletableFuture<CompletableFuture<T>> findNext() {
        return source.hasNext().thenCompose(hasNext -> {
            if (hasNext) {
                return source.next().thenCompose(item -> {
                    if (predicate.test(item)) {
                        nextItem = CompletableFuture.completedFuture(item);
                        return CompletableFuture.completedFuture(nextItem);
                    } else {
                        return findNext();
                    }
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });
    }
}