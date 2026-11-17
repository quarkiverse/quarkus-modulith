package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

@Singleton
public class ApplicationTransport implements EventExternalizationTransport {

    private final List<RoutingTarget> targets = new CopyOnWriteArrayList<>();

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        targets.add(target);
        return CompletableFuture.completedFuture("application");
    }

    public List<RoutingTarget> targets() {
        return List.copyOf(targets);
    }
}
