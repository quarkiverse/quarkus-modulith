package io.quarkiverse.modulith.events.jpa.deployment.externalized;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

@Singleton
public class CapturingTransport implements EventExternalizationTransport {

    public final List<String> targets = new CopyOnWriteArrayList<>();

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        targets.add(target.toString());
        return CompletableFuture.completedFuture("ack");
    }
}
