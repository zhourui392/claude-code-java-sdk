package com.anthropic.claude.streaming;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 异步迭代器接口
 * 支持异步流处理，类似于Java的Iterator但支持异步操作
 *
 * @param <T> 元素类型
 * @author Claude Code Java SDK
 * @version 1.0.0
 */
public interface AsyncIterator<T> {

    /**
     * 异步检查是否有下一个元素
     *
     * @return CompletableFuture包装的布尔值，表示是否有下一个元素
     */
    CompletableFuture<Boolean> hasNext();

    /**
     * 异步获取下一个元素
     *
     * @return CompletableFuture包装的下一个元素
     * @throws java.util.NoSuchElementException 如果没有更多元素
     */
    CompletableFuture<T> next();

    /**
     * 尝试异步获取下一个元素
     *
     * @return CompletableFuture包装的Optional，包含下一个元素或empty
     */
    default CompletableFuture<java.util.Optional<T>> tryNext() {
        return hasNext().thenCompose(hasNext -> {
            if (hasNext) {
                return next().thenApply(java.util.Optional::of);
            } else {
                return CompletableFuture.completedFuture(java.util.Optional.empty());
            }
        });
    }

    /**
     * 异步收集所有剩余元素到列表
     *
     * @return CompletableFuture包装的元素列表
     */
    default CompletableFuture<java.util.List<T>> collectToList() {
        java.util.List<T> result = new java.util.ArrayList<>();
        return collectToList(result);
    }

    /**
     * 异步收集所有剩余元素到指定列表
     *
     * @param list 目标列表
     * @return CompletableFuture包装的元素列表
     */
    default CompletableFuture<java.util.List<T>> collectToList(java.util.List<T> list) {
        return hasNext().thenCompose(hasNext -> {
            if (hasNext) {
                return next().thenCompose(item -> {
                    list.add(item);
                    return collectToList(list);
                });
            } else {
                return CompletableFuture.completedFuture(list);
            }
        });
    }

    /**
     * 异步对每个元素执行操作
     *
     * @param action 要执行的操作
     * @return CompletableFuture表示操作完成
     */
    default CompletableFuture<Void> forEach(java.util.function.Consumer<T> action) {
        return hasNext().thenCompose(hasNext -> {
            if (hasNext) {
                return next().thenCompose(item -> {
                    action.accept(item);
                    return forEach(action);
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    /**
     * 异步对每个元素执行异步操作
     *
     * @param action 要执行的异步操作
     * @return CompletableFuture表示操作完成
     */
    default CompletableFuture<Void> forEachAsync(java.util.function.Function<T, CompletionStage<Void>> action) {
        return hasNext().thenCompose(hasNext -> {
            if (hasNext) {
                return next().thenCompose(item ->
                        action.apply(item).thenCompose(ignored -> forEachAsync(action))
                );
            } else {
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    /**
     * 异步映射每个元素
     *
     * @param mapper 映射函数
     * @param <R> 结果类型
     * @return 映射后的异步迭代器
     */
    default <R> AsyncIterator<R> map(java.util.function.Function<T, R> mapper) {
        return new MappedAsyncIterator<>(this, mapper);
    }

    /**
     * 异步过滤元素
     *
     * @param predicate 过滤条件
     * @return 过滤后的异步迭代器
     */
    default AsyncIterator<T> filter(java.util.function.Predicate<T> predicate) {
        return new FilteredAsyncIterator<>(this, predicate);
    }

    /**
     * 限制元素数量
     *
     * @param limit 最大元素数量
     * @return 限制后的异步迭代器
     */
    default AsyncIterator<T> limit(long limit) {
        return new LimitedAsyncIterator<>(this, limit);
    }

    /**
     * 跳过指定数量的元素
     *
     * @param skip 要跳过的元素数量
     * @return 跳过后的异步迭代器
     */
    default AsyncIterator<T> skip(long skip) {
        return new SkippedAsyncIterator<>(this, skip);
    }
}