package com.anthropic.claude.process;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StreamHandler {
    private static final Logger logger = LoggerFactory.getLogger(StreamHandler.class);

    private final PublishSubject<String> outputSubject;
    private final AtomicBoolean isActive;

    public StreamHandler() {
        this.outputSubject = PublishSubject.create();
        this.isActive = new AtomicBoolean(false);
    }

    public void start() {
        if (isActive.compareAndSet(false, true)) {
            logger.debug("启动流处理器");
        }
    }

    public void stop() {
        if (isActive.compareAndSet(true, false)) {
            logger.debug("停止流处理器");
            outputSubject.onComplete();
        }
    }

    public void handleOutput(String output) {
        if (isActive.get()) {
            outputSubject.onNext(output);
        }
    }

    public void handleError(Throwable error) {
        if (isActive.get()) {
            logger.error("流处理器遇到错误", error);
            outputSubject.onError(error);
        }
    }

    public Observable<String> getOutputStream() {
        return outputSubject.hide();
    }

    public Consumer<String> getOutputConsumer() {
        return this::handleOutput;
    }

    public boolean isActive() {
        return isActive.get();
    }
}