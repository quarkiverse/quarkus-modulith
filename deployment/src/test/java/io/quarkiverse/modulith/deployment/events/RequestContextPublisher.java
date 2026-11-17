package io.quarkiverse.modulith.deployment.events;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Event;

import io.quarkiverse.modulith.deployment.fixtures.events.requestcontext.RequestMarker;
import io.quarkiverse.modulith.deployment.fixtures.events.requestcontext.Visit;
import io.quarkiverse.modulith.deployment.fixtures.events.requestcontext.VisitListeners;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Fires a {@link Visit} from a blocking handler on a duplicated Vert.x context with an active request context, as an
 * HTTP request does, and keeps that request open until the asynchronous listener ran.
 */
final class RequestContextPublisher {

    static final String PUBLISHER = "publisher";

    private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(5);
    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    private RequestContextPublisher() {
    }

    static List<String> publishFromRequest(Vertx vertx, Event<Visit> events, RequestMarker marker, String id)
            throws Exception {
        VisitListeners.SEEN.clear();
        Context context = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        return context.executeBlocking(() -> {
            ManagedContext request = Arc.container().requestContext();
            request.activate();
            try {
                marker.mark(PUBLISHER);
                events.fire(new Visit(id));
                await().atMost(LISTENER_TIMEOUT).until(() -> !VisitListeners.SEEN.isEmpty());
                return List.copyOf(VisitListeners.SEEN);
            } finally {
                request.terminate();
            }
        }).toCompletionStage().toCompletableFuture().get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
