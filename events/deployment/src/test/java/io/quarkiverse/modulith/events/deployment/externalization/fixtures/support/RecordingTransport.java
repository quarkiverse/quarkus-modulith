package io.quarkiverse.modulith.events.deployment.externalization.fixtures.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

@Singleton
public class RecordingTransport implements EventExternalizationTransport {

    private final List<SentMessage> sent = new CopyOnWriteArrayList<>();
    private final AtomicBoolean down = new AtomicBoolean();
    private final AtomicBoolean hanging = new AtomicBoolean();
    private final List<Boolean> virtualThreads = new CopyOnWriteArrayList<>();

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        if (hanging.get()) {
            return new CompletableFuture<>();
        }
        if (down.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("broker down"));
        }
        sent.add(new SentMessage(payload, target, headers));
        virtualThreads.add(Thread.currentThread().isVirtual());
        return CompletableFuture.completedFuture("ack-" + sent.size());
    }

    public List<SentMessage> sent() {
        return List.copyOf(sent);
    }

    public List<Boolean> threads() {
        return List.copyOf(virtualThreads);
    }

    public void down(boolean value) {
        down.set(value);
    }

    public void hang(boolean value) {
        hanging.set(value);
    }

    public void reset() {
        sent.clear();
        virtualThreads.clear();
        down.set(false);
        hanging.set(false);
    }
}
