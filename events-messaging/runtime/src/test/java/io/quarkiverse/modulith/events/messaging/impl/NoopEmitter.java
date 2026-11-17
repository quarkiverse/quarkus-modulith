package io.quarkiverse.modulith.events.messaging.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

class NoopEmitter implements Emitter<Object> {

    @Override
    public CompletionStage<Void> send(Object payload) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <M extends Message<? extends Object>> void send(M message) {
        message.ack();
    }

    @Override
    public void complete() {
    }

    @Override
    public void error(Exception failure) {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean hasRequests() {
        return true;
    }
}
