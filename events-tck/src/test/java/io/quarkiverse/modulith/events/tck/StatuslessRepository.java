package io.quarkiverse.modulith.events.tck;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * A store on a table without status, attempt and resubmission columns, like the legacy JDBC structure: the status is
 * derived from the completion date, attempts are zero, and nothing is ever marked processing, failed or resubmitted.
 */
final class StatuslessRepository extends InMemoryEventPublicationRepository {

    @Override
    public boolean markProcessing(UUID identifier) {
        return isIncomplete(identifier);
    }

    @Override
    public void markFailed(UUID identifier) {
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return isIncomplete(identifier);
    }

    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        return isIncomplete(identifier);
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return withoutStatus(super.findIncompletePublications());
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return withoutStatus(super.findIncompletePublicationsPublishedBefore(instant));
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        return super.findIncompletePublicationsByEventAndTargetIdentifier(event, target).map(this::withoutStatus);
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return withoutStatus(super.findCompletedPublications());
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return List.of();
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return switch (status) {
            case COMPLETED -> findCompletedPublications();
            case PUBLISHED -> findIncompletePublications();
            default -> List.of();
        };
    }

    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        return 0;
    }

    private boolean isIncomplete(UUID identifier) {
        return super.findIncompletePublications().stream().anyMatch(p -> p.getIdentifier().equals(identifier));
    }

    private List<TargetEventPublication> withoutStatus(List<TargetEventPublication> publications) {
        return publications.stream().map(this::withoutStatus).toList();
    }

    private TargetEventPublication withoutStatus(TargetEventPublication publication) {
        return TargetEventPublication.of(publication.getIdentifier(), publication.getEvent(), publication.getEventType(),
                publication.getTargetIdentifier(), publication.getPublicationDate(), publication.getCompletionDate(),
                publication.isCompleted() ? Status.COMPLETED : Status.PUBLISHED, 0, Optional.empty());
    }
}
