package com.anthropic.claude.streaming;

import java.util.concurrent.CompletableFuture;

/**
 * 跳过异步迭代器
 * 跳过指定数量的元素
 *
 * @param <T> 元素类型
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public class SkippedAsyncIterator<T> implements AsyncIterator<T> {

    private final AsyncIterator<T> source;
    private final long skip;
    private long skipped = 0;
    private boolean skipCompleted = false;

    public SkippedAsyncIterator(AsyncIterator<T> source, long skip) {
        this.source = source;
        this.skip = skip;
    }

    @Override
    public CompletableFuture<Boolean> hasNext() {
        if (!skipCompleted) {
            return performSkip().thenCompose(ignored -> source.hasNext());
        }
        return source.hasNext();
    }

    @Override
    public CompletableFuture<T> next() {
        if (!skipCompleted) {
            return performSkip().thenCompose(ignored -> source.next());
        }
        return source.next();
    }

    private CompletableFuture<Void> performSkip() {
        if (skipCompleted || skipped >= skip) {
            skipCompleted = true;
            return CompletableFuture.completedFuture(null);
        }

        return source.hasNext().thenCompose(hasNext -> {
            if (hasNext && skipped < skip) {
                return source.next().thenCompose(ignored -> {
                    skipped++;
                    return performSkip();
                });
            } else {
                skipCompleted = true;
                return CompletableFuture.completedFuture(null);
            }
        });
    }
}