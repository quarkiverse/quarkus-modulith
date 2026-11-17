package io.quarkiverse.modulith.events.tck;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * Implements only the abstract methods, by delegation, so the contract runs against every default method of the SPI,
 * as a store that relies on them would.
 */
final class AbstractMethodsOnlyRepository implements EventPublicationRepository {

    private final InMemoryEventPublicationRepository delegate = new InMemoryEventPublicationRepository();

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        return delegate.create(publication);
    }

    @Override
    public boolean markProcessing(UUID identifier) {
        return delegate.markProcessing(identifier);
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        delegate.markCompleted(identifier, completionDate);
    }

    @Override
    public void markFailed(UUID identifier) {
        delegate.markFailed(identifier);
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return delegate.markResubmitted(identifier, resubmissionDate);
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return delegate.findIncompletePublications();
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return delegate.findIncompletePublicationsPublishedBefore(instant);
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        return delegate.findIncompletePublicationsByEventAndTargetIdentifier(event, target);
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return delegate.findCompletedPublications();
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return delegate.findFailedPublications(criteria);
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return delegate.findByStatus(status);
    }

    @Override
    public int countByStatus(Status status) {
        return delegate.countByStatus(status);
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        delegate.deletePublications(identifiers);
    }

    @Override
    public void deleteCompletedPublications() {
        delegate.deleteCompletedPublications();
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        delegate.deleteCompletedPublicationsBefore(instant);
    }
}
