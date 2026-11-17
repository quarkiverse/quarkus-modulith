package io.quarkiverse.modulith.events.jpa.deployment.tck;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Runs each call of the contract in its own transaction, the way the registry calls the store.
 */
public final class TransactionalRepository implements EventPublicationRepository {

    private final EventPublicationRepository delegate;

    public TransactionalRepository(EventPublicationRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.create(publication));
    }

    @Override
    public boolean markProcessing(UUID identifier) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.markProcessing(identifier));
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        QuarkusTransaction.requiringNew().run(() -> delegate.markCompleted(identifier, completionDate));
    }

    @Override
    public void markFailed(UUID identifier) {
        QuarkusTransaction.requiringNew().run(() -> delegate.markFailed(identifier));
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.markResubmitted(identifier, resubmissionDate));
    }

    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.claim(identifier, eligible, staleBefore, claimDate));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        return QuarkusTransaction.requiringNew()
                .call(() -> delegate.findIncompletePublications(listener, statuses, criteria));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return QuarkusTransaction.requiringNew().call(delegate::findIncompletePublications);
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.findIncompletePublicationsPublishedBefore(instant));
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        return QuarkusTransaction.requiringNew()
                .call(() -> delegate.findIncompletePublicationsByEventAndTargetIdentifier(event, target));
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return QuarkusTransaction.requiringNew().call(delegate::findCompletedPublications);
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.findFailedPublications(criteria));
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.findByStatus(status));
    }

    @Override
    public int countByStatus(Status status) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.countByStatus(status));
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        QuarkusTransaction.requiringNew().run(() -> delegate.deletePublications(identifiers));
    }

    @Override
    public int deletePublicationsCounting(List<UUID> identifiers) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.deletePublicationsCounting(identifiers));
    }

    @Override
    public void deleteCompletedPublications() {
        QuarkusTransaction.requiringNew().run(delegate::deleteCompletedPublications);
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        QuarkusTransaction.requiringNew().run(() -> delegate.deleteCompletedPublicationsBefore(instant));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.findIncompletePublications(criteria));
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications(PageCriteria criteria) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.findCompletedPublications(criteria));
    }

    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        return QuarkusTransaction.requiringNew()
                .call(() -> delegate.markStalePublicationsFailed(status, lastPublishedBefore));
    }

    @Override
    public int deleteCompletedPublicationsCompletedBefore(Instant instant) {
        return QuarkusTransaction.requiringNew().call(() -> delegate.deleteCompletedPublicationsCompletedBefore(instant));
    }
}
