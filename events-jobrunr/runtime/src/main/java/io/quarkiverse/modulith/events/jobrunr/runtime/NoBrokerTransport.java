package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

/**
 * Stands in for the broker transport when the application externalizes events only through its own
 * {@code JobRunrExternalizationTransport} beans. Sending through it fails.
 */
@Singleton
public class NoBrokerTransport implements EventExternalizationTransport {

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        return CompletableFuture.failedFuture(new IllegalStateException("No broker transport is present: events are"
                + " externalized through the JobRunrExternalizationTransport beans of the application only. Add a"
                + " broker transport such as quarkus-modulith-events-jms to send events through EventExternalizer"));
    }
}
