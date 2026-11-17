package io.quarkiverse.modulith.it.externalizationjobrunr.delivery;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

/**
 * Stands in for a broker transport such as JMS. JobRunr reaches it through the transport the extension registers.
 */
@Singleton
public class BrokerTransport implements EventExternalizationTransport {

    final Recorder recorder = new Recorder();

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        try {
            recorder.record(target.toString(), payload);
            return CompletableFuture.completedFuture("ack-" + recorder.deliveries().size());
        } catch (IllegalStateException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
