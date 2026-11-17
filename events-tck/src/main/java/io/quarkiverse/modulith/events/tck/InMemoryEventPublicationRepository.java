package io.quarkiverse.modulith.events.tck;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PublicationCursor;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * An {@link EventPublicationRepository} that keeps publications in memory. The reference implementation of the
 * contract, and a stand-in for tests of code built on the repository before a store is available. Not for
 * production: publications are lost when the application stops.
 */
public class InMemoryEventPublicationRepository implements EventPublicationRepository {

    private final Map<UUID, TargetEventPublication> publications = new LinkedHashMap<>();
    private final Map<UUID, TargetEventPublication> archive = new LinkedHashMap<>();
    private final CompletionMode completionMode;

    /**
     * A repository that keeps completed publications, completion mode {@code UPDATE}.
     */
    public InMemoryEventPublicationRepository() {
        this(CompletionMode.UPDATE);
    }

    /**
     * @param completionMode what happens to completed publications
     */
    public InMemoryEventPublicationRepository(CompletionMode completionMode) {
        this.completionMode = completionMode;
    }

    @Override
    public synchronized TargetEventPublication create(TargetEventPublication publication) {
        publications.put(publication.getIdentifier(), publication);
        return publication;
    }

    @Override
    public synchronized boolean markProcessing(UUID identifier) {
        TargetEventPublication current = publications.get(identifier);
        if (Objects.isNull(current) || current.isCompleted()) {
            return false;
        }
        replace(current, Optional.empty(), Status.PROCESSING, current.getCompletionAttempts() + 1,
                current.getLastResubmissionDate());
        return true;
    }

    @Override
    public synchronized void markCompleted(UUID identifier, Instant completionDate) {
        TargetEventPublication current = publications.get(identifier);
        if (Objects.isNull(current)) {
            return;
        }
        replace(current, Optional.of(completionDate), Status.COMPLETED, current.getCompletionAttempts(),
                current.getLastResubmissionDate());
        if (completionMode != CompletionMode.UPDATE) {
            TargetEventPublication completed = publications.remove(identifier);
            if (completionMode == CompletionMode.ARCHIVE) {
                archive.put(identifier, completed);
            }
        }
    }

    @Override
    public synchronized void markFailed(UUID identifier) {
        TargetEventPublication current = publications.get(identifier);
        if (Objects.nonNull(current) && !current.isCompleted()) {
            replace(current, current.getCompletionDate(), Status.FAILED, current.getCompletionAttempts(),
                    current.getLastResubmissionDate());
        }
    }

    @Override
    public synchronized boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        TargetEventPublication current = publications.get(identifier);
        if (Objects.isNull(current) || current.isCompleted() || current.getStatus() == Status.RESUBMITTED) {
            return false;
        }
        replace(current, Optional.empty(), Status.RESUBMITTED, current.getCompletionAttempts(),
                Optional.of(resubmissionDate));
        return true;
    }

    @Override
    public synchronized boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        TargetEventPublication current = publications.get(identifier);
        if (Objects.isNull(current) || current.isCompleted()) {
            return false;
        }
        boolean stale = STALEABLE.contains(current.getStatus()) && current.getLastPublicationDate().isBefore(staleBefore);
        if (!eligible.contains(current.getStatus()) && !stale) {
            return false;
        }
        replace(current, Optional.empty(), Status.RESUBMITTED, current.getCompletionAttempts(), Optional.of(claimDate));
        return true;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return find(p -> !p.isCompleted());
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return find(p -> !p.isCompleted() && p.getPublicationDate().isBefore(instant));
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        return find(p -> !p.isCompleted() && p.getTargetIdentifier().equals(target) && p.getEvent().equals(event))
                .stream().findFirst();
    }

    @Override
    public synchronized List<TargetEventPublication> findCompletedPublications() {
        return completed().values().stream().filter(TargetEventPublication::isCompleted).sorted(PublicationCursor.ORDER)
                .toList();
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return find(p -> p.getStatus() == Status.FAILED && p.getPublicationDate().isBefore(criteria.publishedBefore()))
                .stream().limit(criteria.maxItems()).toList();
    }

    @Override
    public synchronized List<TargetEventPublication> findByStatus(Status status) {
        Map<UUID, TargetEventPublication> source = status == Status.COMPLETED ? completed() : publications;
        return source.values().stream().filter(p -> p.getStatus() == status).sorted(PublicationCursor.ORDER).toList();
    }

    @Override
    public int countByStatus(Status status) {
        return findByStatus(status).size();
    }

    @Override
    public synchronized void deletePublications(List<UUID> identifiers) {
        identifiers.forEach(publications::remove);
        identifiers.forEach(archive::remove);
    }

    @Override
    public synchronized void deleteCompletedPublications() {
        completed().values().removeIf(TargetEventPublication::isCompleted);
    }

    @Override
    public synchronized void deleteCompletedPublicationsBefore(Instant instant) {
        completed().values().removeIf(p -> p.getCompletionDate().filter(date -> date.isBefore(instant)).isPresent());
    }

    private Map<UUID, TargetEventPublication> completed() {
        return completionMode == CompletionMode.ARCHIVE ? archive : publications;
    }

    private synchronized List<TargetEventPublication> find(Predicate<TargetEventPublication> filter) {
        return publications.values().stream().filter(filter).sorted(PublicationCursor.ORDER).toList();
    }

    private void replace(TargetEventPublication current, Optional<Instant> completionDate, Status status, int attempts,
            Optional<Instant> lastResubmissionDate) {
        publications.put(current.getIdentifier(), TargetEventPublication.of(current.getIdentifier(), current.getEvent(),
                current.getEventType(), current.getTargetIdentifier(), current.getPublicationDate(), completionDate,
                status, attempts, lastResubmissionDate));
    }
}
