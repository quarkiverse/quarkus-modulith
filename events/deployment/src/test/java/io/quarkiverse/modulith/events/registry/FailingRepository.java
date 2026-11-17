package io.quarkiverse.modulith.events.registry;

import java.util.List;

import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * A store whose connection is gone.
 */
final class FailingRepository extends InMemoryEventPublicationRepository {

    @Override
    public List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        throw new IllegalStateException("connection closed");
    }
}
