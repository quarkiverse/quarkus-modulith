package io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

public class CustomTransport implements EventExternalizationTransport {

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        return CompletableFuture.completedFuture(null);
    }
}
