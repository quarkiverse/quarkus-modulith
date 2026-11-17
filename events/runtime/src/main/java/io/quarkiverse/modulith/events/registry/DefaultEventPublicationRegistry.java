package io.quarkiverse.modulith.events.registry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.FailedEventPublications;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.ResubmissionOptions;
import io.quarkiverse.modulith.events.config.EventsConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.ResubmissionExclusion;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.spi.EventPublicationRegistry;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkiverse.modulith.events.spi.SerializationFailures;
import io.quarkiverse.modulith.events.spi.TransactionRunner;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ShutdownEvent;

/**
 * The event publication registry, built on the {@link EventPublicationRepository} of the store in use, like Spring
 * Modulith's {@code DefaultEventPublicationRegistry}. It stores publications for the dispatcher, drives their
 * lifecycle ({@code PUBLISHED}, {@code PROCESSING}, {@code COMPLETED} or {@code FAILED}, {@code RESUBMITTED}), and is
 * the bean behind {@link IncompleteEventPublications}, {@link CompletedEventPublications},
 * {@link FailedEventPublications}, and through {@link DefaultAbandonedEventPublications} of
 * {@link io.quarkiverse.modulith.events.AbandonedEventPublications}.
 * <p>
 * Every read of a potentially large set goes page by page, and resubmission claims each publication with a
 * conditional update first, so that several instances sharing a database resubmit each publication once.
 */
@Singleton
public class DefaultEventPublicationRegistry implements EventPublicationRegistry, IncompleteEventPublications,
        CompletedEventPublications, FailedEventPublications {

    static final int SHUTDOWN_REPORT_SIZE = 100;
    static final Set<Status> ELIGIBLE = Set.of(Status.PUBLISHED, Status.FAILED);
    static final Set<Status> FAILED_ONLY = Set.of(Status.FAILED);
    private static final Logger LOG = Logger.getLogger(DefaultEventPublicationRegistry.class);
    private static final List<Status> MONITORED = List.of(Status.PUBLISHED, Status.PROCESSING, Status.RESUBMITTED);
    private static final List<Status> IN_FLIGHT = List.of(Status.RESUBMITTED, Status.PROCESSING);

    @Inject
    EventPublicationRepository repository;

    @Inject
    TransactionRunner transactions;

    @Inject
    Instance<EventRedelivery> redelivery;

    @Inject
    Instance<Clock> clocks;

    @Inject
    Instance<ResubmissionExclusion> exclusionBeans;

    List<ResubmissionExclusion> exclusions = List.of();

    @Inject
    EventsConfig config;

    Clock clock = Clock.systemUTC();
    Optional<EventRedelivery> fixedRedelivery = Optional.empty();

    /**
     * For CDI.
     */
    public DefaultEventPublicationRegistry() {
    }

    /**
     * A registry outside CDI, for example a second instance sharing the same store in a test, or a tool that
     * resubmits publications of another deployment.
     */
    public DefaultEventPublicationRegistry(EventPublicationRepository repository, TransactionRunner transactions,
            EventsConfig config, Clock clock, EventRedelivery redelivery) {
        this.repository = repository;
        this.transactions = transactions;
        this.config = config;
        this.clock = clock;
        this.fixedRedelivery = Optional.of(redelivery);
    }

    /**
     * Uses the application's {@link Clock} bean when there is exactly one.
     */
    @PostConstruct
    void init() {
        if (clocks.isResolvable()) {
            clock = clocks.get();
        }
        exclusions = exclusionBeans.stream().toList();
    }

    @Override
    public List<UUID> store(Object event, List<String> listenerIds) {
        return transactions.callInTransaction(() -> {
            Instant now = clock.instant();
            List<UUID> ids = new ArrayList<>(listenerIds.size());
            for (String listenerId : listenerIds) {
                ids.add(repository.create(TargetEventPublication.of(event, PublicationTargetIdentifier.of(listenerId),
                        now)).getIdentifier());
            }
            return ids;
        });
    }

    /**
     * Lets the store decide, see {@link EventPublicationRepository#afterCommit}.
     */
    @Override
    public void afterCommit(Runnable deliveries, Consumer<Runnable> afterTransactionCommit) {
        repository.afterCommit(deliveries, afterTransactionCommit);
    }

    @Override
    public boolean markProcessing(UUID publicationId) {
        return repository.markProcessing(publicationId);
    }

    @Override
    public void markCompleted(UUID publicationId) {
        repository.markCompleted(publicationId, clock.instant());
    }

    @Override
    public void markFailed(UUID publicationId, Throwable failure) {
        repository.markFailed(publicationId);
    }

    @Override
    public List<EventPublication> findIncompletePublications() {
        requireBlockingAllowed();
        return transactions.callInNewTransaction(() -> List.copyOf(repository.findIncompletePublications()));
    }

    @Override
    public int resubmitIncompletePublications(Predicate<EventPublication> filter) {
        requireBlockingAllowed();
        return resubmitIncomplete(EventPublicationRepository.FAR_FUTURE, filter);
    }

    @Override
    public int resubmitIncompletePublicationsOlderThan(Duration duration) {
        requireBlockingAllowed();
        return resubmitIncomplete(clock.instant().minus(duration), publication -> true);
    }

    @Override
    public int resubmitIncompletePublications(ResubmissionOptions options) {
        return resubmit(options);
    }

    /**
     * Publications in flight are those claimed ({@code RESUBMITTED}) and those being delivered ({@code PROCESSING}),
     * since every delivery marks its publication processing as it starts. Spring Modulith 2.1.2 counts
     * {@code RESUBMITTED} only, which its deliveries keep until they finish.
     */
    @Override
    public int resubmit(ResubmissionOptions options) {
        requireBlockingAllowed();
        int inFlight = transactions.callInNewTransaction(
                () -> IN_FLIGHT.stream().mapToInt(repository::countByStatus).sum());
        if (inFlight >= options.getMaxInFlight()) {
            LOG.infof("Skipping resubmission: %d publications are in flight already, at most %d may be",
                    inFlight, options.getMaxInFlight());
            return 0;
        }
        FailedCriteria criteria = new FailedCriteria(Math.min(options.getBatchSize(), options.getMaxInFlight() - inFlight),
                clock.instant().minus(options.getMinAge()));
        List<TargetEventPublication> failed = transactions
                .callInNewTransaction(() -> repository.findFailedPublications(criteria));
        int submitted = 0;
        for (TargetEventPublication publication : failed) {
            if (isResubmittable(publication) && options.getFilter().test(publication)
                    && resubmit(publication, FAILED_ONLY)) {
                submitted++;
            }
        }
        return logged(submitted);
    }

    @Override
    public List<EventPublication> findAll() {
        requireBlockingAllowed();
        return transactions.callInNewTransaction(() -> List.copyOf(repository.findCompletedPublications()));
    }

    @Override
    public int deletePublications(Predicate<EventPublication> filter) {
        requireBlockingAllowed();
        int deleted = 0;
        PageCriteria criteria = PageCriteria.first(EventPublicationRepository.FAR_FUTURE, config.republishBatchSize());
        List<TargetEventPublication> page;
        do {
            page = completedPage(criteria);
            List<UUID> ids = page.stream().filter(filter).map(TargetEventPublication::getIdentifier).toList();
            deleted += delete(ids);
            criteria = next(criteria, page);
        } while (page.size() == criteria.maxItems());
        return deleted;
    }

    @Override
    public int deletePublicationsOlderThan(Duration duration) {
        requireBlockingAllowed();
        Instant cutoff = clock.instant().minus(duration);
        return transactions.callInNewTransaction(() -> repository.deleteCompletedPublicationsCompletedBefore(cutoff));
    }

    /**
     * @return the incomplete publications at the configured attempt limit, oldest first
     */
    public List<EventPublication> findAbandoned() {
        requireBlockingAllowed();
        List<EventPublication> abandoned = new ArrayList<>();
        forEachAbandonedPage(abandoned::addAll);
        return abandoned;
    }

    /**
     * Deletes the abandoned publications matching the filter, page by page.
     *
     * @return the number of deleted publications
     */
    public int deleteAbandoned(Predicate<EventPublication> filter) {
        requireBlockingAllowed();
        AtomicInteger deleted = new AtomicInteger();
        forEachAbandonedPage(page -> deleted.addAndGet(
                delete(page.stream().filter(filter).map(EventPublication::getIdentifier).toList())));
        return deleted.get();
    }

    /**
     * Marks publications that stayed too long in a monitored status as failed.
     *
     * @return the number of publications marked failed
     */
    public int markStalePublicationsFailed() {
        requireBlockingAllowed();
        int total = 0;
        Instant now = clock.instant();
        for (Status status : MONITORED) {
            for (Duration threshold : threshold(status).stream().toList()) {
                int marked = transactions.callInNewTransaction(
                        () -> repository.markStalePublicationsFailed(status, now.minus(threshold)));
                if (marked > 0) {
                    LOG.infof("Marked %d stale publication(s) with status %s older than %s as failed", marked, status,
                            threshold);
                }
                total += marked;
            }
        }
        return total;
    }

    /**
     * Logs the publications left incomplete, after the dispatcher stopped taking deliveries, like Spring Modulith
     * does when its registry is destroyed. Only event types and listener identifiers are logged.
     */
    void onShutdown(@Observes @Priority(200) ShutdownEvent event) {
        try {
            List<TargetEventPublication> left = incompletePage(
                    PageCriteria.first(EventPublicationRepository.FAR_FUTURE, SHUTDOWN_REPORT_SIZE));
            if (left.isEmpty()) {
                LOG.info("No publications outstanding");
                return;
            }
            LOG.infof("Shutting down with %s publication(s) left unfinished:",
                    left.size() == SHUTDOWN_REPORT_SIZE ? "at least " + SHUTDOWN_REPORT_SIZE : left.size());
            for (int i = 0; i < left.size(); i++) {
                TargetEventPublication publication = left.get(i);
                LOG.infof("%s %s - %s", i + 1 == left.size() ? "└─" : "├─", publication.getEventType(),
                        publication.getListenerId());
            }
        } catch (RuntimeException e) {
            LOG.warnf("Could not list the publications left unfinished at shutdown: %s", e.getMessage());
        }
    }

    /**
     * Reads the incomplete publications page by page and hands the abandoned ones of each page to the action, none
     * without an attempt limit. Deleting from a page does not move the cursor, which is the last row read.
     */
    private void forEachAbandonedPage(Consumer<List<TargetEventPublication>> action) {
        if (config.resubmission().abandonAfterAttempts().isEmpty()) {
            return;
        }
        PageCriteria criteria = PageCriteria.first(EventPublicationRepository.FAR_FUTURE, config.republishBatchSize());
        List<TargetEventPublication> page;
        do {
            page = incompletePage(criteria);
            action.accept(page.stream().filter(this::isAbandoned).toList());
            criteria = next(criteria, page);
        } while (page.size() == criteria.maxItems());
    }

    private int resubmitIncomplete(Instant publishedBefore, Predicate<EventPublication> filter) {
        int submitted = 0;
        PageCriteria criteria = PageCriteria.first(publishedBefore, config.republishBatchSize());
        List<TargetEventPublication> page;
        do {
            page = incompletePage(criteria);
            for (TargetEventPublication publication : page) {
                if (isResubmittable(publication) && filter.test(publication) && resubmit(publication, ELIGIBLE)) {
                    submitted++;
                }
            }
            criteria = next(criteria, page);
        } while (page.size() == criteria.maxItems());
        return logged(submitted);
    }

    /**
     * Claims the publication and hands it to its listener. A publication whose listener no longer exists is marked
     * failed, as in Spring Modulith. A publication that cannot be handed over, because its event cannot be read or the
     * dispatcher refuses it, is marked failed with one more attempt, so that it is not left claimed and
     * {@code abandon-after-attempts} eventually applies to it.
     */
    private boolean resubmit(TargetEventPublication publication, Set<Status> eligible) {
        UUID id = publication.getIdentifier();
        if (!claim(id, eligible)) {
            return false;
        }
        try {
            Optional<EventRedelivery> target = redelivery();
            if (target.isPresent() && target.get().redeliver(id, publication.getListenerId(), publication.getEvent())) {
                return true;
            }
            LOG.errorf("Listener %s not found, leaving event publication %s failed", publication.getListenerId(), id);
            transactions.callInNewTransaction(() -> {
                repository.markFailed(id);
                return null;
            });
        } catch (RuntimeException e) {
            LOG.warnf(e, "Could not resubmit event publication %s, marking it failed", id);
            markFailedAfterAttempt(id);
        }
        return false;
    }

    private void markFailedAfterAttempt(UUID id) {
        try {
            transactions.callInNewTransaction(() -> {
                repository.markProcessing(id);
                repository.markFailed(id);
                return null;
            });
        } catch (RuntimeException e) {
            LOG.warnf(e, "Could not mark event publication %s failed, it is taken over once stale", id);
        }
    }

    /**
     * The registry reads and writes the store synchronously, which must not happen on a Vert.x I/O thread, whatever
     * the store. Publishing is moved off I/O threads by the dispatcher already.
     */
    void requireBlockingAllowed() {
        if (!isBlockingAllowed()) {
            throw new IllegalStateException("The event publication registry blocks on its store, call it from a worker"
                    + " or virtual thread, not from an I/O thread");
        }
    }

    /**
     * A seam for unit tests.
     */
    boolean isBlockingAllowed() {
        return BlockingOperationControl.isBlockingAllowed();
    }

    /**
     * Looked up per resubmission, not at startup: the dispatcher itself looks up the registry while it is created.
     */
    Optional<EventRedelivery> redelivery() {
        return fixedRedelivery.or(() -> redelivery.stream().findFirst());
    }

    /**
     * A serialization failure means another instance claimed the publication concurrently.
     */
    private boolean claim(UUID id, Set<Status> eligible) {
        Instant now = clock.instant();
        Instant staleBefore = now.minus(staleClaimThreshold());
        try {
            return transactions.callInNewTransaction(() -> repository.claim(id, eligible, staleBefore, now));
        } catch (RuntimeException e) {
            if (SerializationFailures.isSerializationFailure(e)) {
                LOG.debugf("Event publication %s was claimed concurrently", id);
                return false;
            }
            throw e;
        }
    }

    /**
     * Abandoned publications and those of listeners another component delivers are left alone.
     */
    private boolean isResubmittable(TargetEventPublication publication) {
        return !isAbandoned(publication)
                && exclusions.stream().noneMatch(exclusion -> exclusion.excludes(publication.getTargetIdentifier()));
    }

    /**
     * A claimed or processing publication is taken over when it is older than the processing staleness threshold, or
     * else the resubmitted one, or else {@code republish-older-than}, so that a delivery whose instance died is never
     * lost, also with the default configuration.
     */
    Duration staleClaimThreshold() {
        EventsConfig.Staleness staleness = config.staleness();
        return staleness.processing().or(staleness::resubmitted).orElse(config.republishOlderThan());
    }

    private boolean isAbandoned(TargetEventPublication publication) {
        return config.resubmission().abandonAfterAttempts().stream()
                .anyMatch(limit -> publication.getCompletionAttempts() >= limit);
    }

    private Optional<Duration> threshold(Status status) {
        EventsConfig.Staleness staleness = config.staleness();
        return switch (status) {
            case PUBLISHED -> staleness.published();
            case PROCESSING -> staleness.processing();
            default -> staleness.resubmitted();
        };
    }

    private List<TargetEventPublication> incompletePage(PageCriteria criteria) {
        return transactions.callInNewTransaction(() -> repository.findIncompletePublications(criteria));
    }

    private List<TargetEventPublication> completedPage(PageCriteria criteria) {
        return transactions.callInNewTransaction(() -> repository.findCompletedPublications(criteria));
    }

    private int delete(List<UUID> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        return transactions.callInNewTransaction(() -> repository.deletePublicationsCounting(ids));
    }

    private static PageCriteria next(PageCriteria criteria, List<TargetEventPublication> page) {
        return page.isEmpty() ? criteria : criteria.next(page.get(page.size() - 1));
    }

    private static int logged(int submitted) {
        if (submitted > 0) {
            LOG.infof("Submitted %d event publication(s) again", submitted);
        }
        return submitted;
    }
}
