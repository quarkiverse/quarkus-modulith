package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.List;
import java.util.UUID;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;

class RecordingStrategy implements ExternalizationStrategy {

    private final String listenerId;
    private final List<String> resubmitted;

    RecordingStrategy(String listenerId, List<String> resubmitted) {
        this.listenerId = listenerId;
        this.resubmitted = resubmitted;
    }

    @Override
    public void onEvent(Object event) {
        resubmitted.add("event " + event);
    }

    @Override
    public boolean handles(String listenerId) {
        return this.listenerId.equals(listenerId);
    }

    @Override
    public void resubmit(UUID publicationId, Object event) {
        resubmitted.add(publicationId + " " + event);
    }
}
