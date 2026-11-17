package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The event publications of externalized events, kept through the {@link EventPublicationRepository} of the
 * application when there is one. Without a repository, externalization works without guarantees: nothing is stored
 * and failed sends are only logged.
 * <p>
 * State changes after the publishing transaction run in a transaction of their own.
 */
@Singleton
public class ExternalizationPublications {

    private static final Set<Status> ELIGIBLE = Set.of(Status.PUBLISHED, Status.FAILED);
    private static final Set<Status> PENDING = Set.of(Status.PUBLISHED, Status.FAILED, Status.RESUBMITTED,
            Status.PROCESSING);

    @Inject
    Instance<EventPublicationRepository> repositories;

    @Inject
    Instance<Clock> clocks;

    EventPublicationRepository repository;
    Clock clock;

    @PostConstruct
    void init() {
        repository = repositories.isResolvable() ? repositories.get() : null;
        clock = clocks.isResolvable() ? clocks.get() : Clock.systemUTC();
    }

    /**
     * @return whether publications are stored
     */
    public boolean isPersistent() {
        return Objects.nonNull(repository);
    }

    /**
     * Stores a publication of the event for the listener, in the caller's transaction, or in a transaction of its own
     * when the event was published outside a transaction, which stores such as JPA require.
     *
     * @return the publication identifier, empty without repository
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public Optional<UUID> store(Object event, String listenerId) {
        if (!isPersistent()) {
            return Optional.empty();
        }
        TargetEventPublication publication = TargetEventPublication.of(event, PublicationTargetIdentifier.of(listenerId),
                clock.instant());
        return Optional.of(repository.create(publication).getIdentifier());
    }

    /**
     * Marks the publication as being sent.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markProcessing(UUID publicationId) {
        repository.markProcessing(publicationId);
    }

    /**
     * Marks the publication as sent.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markCompleted(UUID publicationId) {
        repository.markCompleted(publicationId, clock.instant());
    }

    /**
     * Marks the publication as failed, so that it can be sent again.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markFailed(UUID publicationId) {
        repository.markFailed(publicationId);
    }

    /**
     * Claims the publication for sending with a conditional update, so only one caller succeeds, also across
     * instances. {@code PUBLISHED} and {@code FAILED} publications are eligible, and claimed or processing ones whose
     * last claim is older than {@code staleAfter}, whose sender stopped. A lost race can also show as a serialization
     * failure, which callers treat as not claimed.
     *
     * @return whether this call claimed it
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean claim(UUID publicationId, Duration staleAfter) {
        Instant now = clock.instant();
        return repository.claim(publicationId, ELIGIBLE, now.minus(staleAfter), now);
    }

    /**
     * @return one page of the publications of the listener that are not completed, oldest first
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<TargetEventPublication> findPending(String listenerId, PageCriteria page) {
        return repository.findIncompletePublications(PublicationTargetIdentifier.of(listenerId), PENDING, page);
    }
}
