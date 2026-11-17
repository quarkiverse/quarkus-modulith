package io.quarkiverse.modulith.events.externalization.impl;

import java.util.List;
import java.util.UUID;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;

class ResubmittingStrategy implements ExternalizationStrategy {

    private final List<UUID> resubmitted;

    ResubmittingStrategy(List<UUID> resubmitted) {
        this.resubmitted = resubmitted;
    }

    @Override
    public void onEvent(Object event) {
        resubmitted.clear();
    }

    @Override
    public boolean handles(String listenerId) {
        return true;
    }

    @Override
    public void resubmit(UUID publicationId, Object event) {
        resubmitted.add(publicationId);
    }
}
