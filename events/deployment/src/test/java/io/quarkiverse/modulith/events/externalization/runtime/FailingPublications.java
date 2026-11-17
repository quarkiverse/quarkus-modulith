package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

class FailingPublications extends ExternalizationPublications {

    private final String failingStep;

    FailingPublications(String failingStep) {
        this.failingStep = failingStep;
    }

    private void fail(String step) {
        if (failingStep.equals(step)) {
            throw new IllegalStateException(step + " failed");
        }
    }

    @Override
    public Optional<UUID> store(Object event, String listenerId) {
        fail("store");
        return super.store(event, listenerId);
    }

    @Override
    public void markProcessing(UUID publicationId) {
        fail("processing");
        super.markProcessing(publicationId);
    }

    @Override
    public void markCompleted(UUID publicationId) {
        fail("completed");
        super.markCompleted(publicationId);
    }

    @Override
    public boolean claim(UUID publicationId, Duration staleAfter) {
        return !failingStep.equals("claim") && super.claim(publicationId, staleAfter);
    }

    @Override
    public List<TargetEventPublication> findPending(String listenerId, PageCriteria page) {
        fail("find");
        return super.findPending(listenerId, page);
    }
}
