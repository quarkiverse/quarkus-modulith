package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

@Singleton
public class RecordingBrokerTransport implements EventExternalizationTransport {

    private final List<SentMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        sent.add(new SentMessage(payload, target));
        return CompletableFuture.completedFuture("ack-" + sent.size());
    }

    public List<SentMessage> sent() {
        return List.copyOf(sent);
    }

    public void reset() {
        sent.clear();
    }
}
