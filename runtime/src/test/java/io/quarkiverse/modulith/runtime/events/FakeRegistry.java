package io.quarkiverse.modulith.runtime.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.quarkiverse.modulith.events.spi.EventPublicationRegistry;

/**
 * Hands out predictable publication identifiers and records the lifecycle calls.
 */
final class FakeRegistry implements EventPublicationRegistry {

    final List<String> calls = new ArrayList<>();
    boolean claimable = true;
    RuntimeException claimFailure;
    boolean failMarkFailed;
    boolean failMarkCompleted;
    private int next;

    @Override
    public List<UUID> store(Object event, List<String> listenerIds) {
        List<UUID> ids = new ArrayList<>();
        for (String listenerId : listenerIds) {
            calls.add("store " + listenerId);
            ids.add(new UUID(0, ++next));
        }
        return ids;
    }

    @Override
    public boolean markProcessing(UUID publicationId) {
        calls.add("processing " + publicationId.getLeastSignificantBits());
        if (Objects.nonNull(claimFailure)) {
            throw claimFailure;
        }
        return claimable;
    }

    @Override
    public void markCompleted(UUID publicationId) {
        calls.add("completed " + publicationId.getLeastSignificantBits());
        if (failMarkCompleted) {
            throw new IllegalStateException("store down");
        }
    }

    @Override
    public void markFailed(UUID publicationId, Throwable failure) {
        calls.add("failed " + publicationId.getLeastSignificantBits() + " " + failure.getMessage());
        if (failMarkFailed) {
            throw new IllegalStateException("store down");
        }
    }
}
