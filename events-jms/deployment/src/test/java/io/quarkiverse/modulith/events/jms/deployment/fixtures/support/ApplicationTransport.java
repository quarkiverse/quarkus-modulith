package io.quarkiverse.modulith.events.jms.deployment.fixtures.support;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

@Singleton
public class ApplicationTransport implements EventExternalizationTransport {

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        return CompletableFuture.completedFuture("application");
    }
}
