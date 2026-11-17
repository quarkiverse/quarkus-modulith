package io.quarkiverse.modulith.events.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoSessions;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.PanacheMongoSessions;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkus.mongodb.panache.common.runtime.MongoOperations;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class SharedSessionTest {

    private static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("shipping.on-parcel");
    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Set<Status> ELIGIBLE = Set.of(Status.PUBLISHED);

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Parcel.class, TckEvent.class, RunAfterCommit.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.devservices.image-name", "mongo:7.0.48")
            .overrideConfigKey("quarkus.mongodb.database", "shop");

    @Inject
    EventPublicationRepository repository;

    @Inject
    MongoSessions sessions;

    @Inject
    MongoStore store;

    @Inject
    MongoClient client;

    @Inject
    TransactionSynchronizationRegistry registry;

    private ExecutorService otherThread;

    @BeforeEach
    void dropDatabase() {
        client.getDatabase("shop").drop();
        otherThread = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void stopTheOtherThread() {
        otherThread.shutdownNow();
    }

    @Test
    void sharesTheSessionOfPanache() {
        // act
        ClientSession[] seen = QuarkusTransaction.requiringNew().call(() -> {
            parcel("first").persist();
            return new ClientSession[] { sessions.current().orElseThrow(),
                    (ClientSession) registry.getResource(MongoOperations.SESSION_KEY) };
        });

        // assert
        assertThat(sessions).isInstanceOf(PanacheMongoSessions.class);
        assertThat(seen[0]).isSameAs(seen[1]);
        assertThat(store.database().getName()).isEqualTo("shop");
    }

    @Test
    void commitsPublicationsWithTheEntities() {
        // act
        QuarkusTransaction.requiringNew().run(() -> {
            parcel("committed").persist();
            repository.create(publication("committed"));
        });

        // assert
        assertThat(repository.findIncompletePublications()).extracting(TargetEventPublication::getEvent)
                .containsExactly(new TckEvent("committed"));
        assertThat(Parcel.count()).isEqualTo(1);
    }

    @Test
    void rollsPublicationsBackWithTheEntities() {
        // act
        Throwable thrown = catchThrowable(() -> QuarkusTransaction.requiringNew().run(() -> {
            repository.create(publication("rolled-back"));
            parcel("rolled-back").persist();
            throw new IllegalStateException("payment declined");
        }));

        // assert
        assertThat(thrown).hasMessage("payment declined");
        assertThat(repository.findIncompletePublications()).isEmpty();
        assertThat(Parcel.count()).isZero();
    }

    @Test
    void rollsPublicationsBackWithTheSessionOfATransactionMarkedRollbackOnly() {
        // act
        Throwable thrown = catchThrowable(() -> QuarkusTransaction.requiringNew().run(() -> {
            parcel("doomed").persist();
            QuarkusTransaction.setRollbackOnly();
            repository.create(publication("doomed"));
        }));

        // assert
        assertThat(thrown).hasCauseInstanceOf(RollbackException.class);
        assertThat(repository.findIncompletePublications()).isEmpty();
        assertThat(Parcel.count()).isZero();
    }

    @Test
    void dropsThePublicationOfATransactionMarkedRollbackOnlyWithoutSession() {
        // act
        Throwable thrown = catchThrowable(() -> QuarkusTransaction.requiringNew().run(() -> {
            QuarkusTransaction.setRollbackOnly();
            repository.create(publication("doomed"));
        }));

        // assert
        assertThat(thrown).hasCauseInstanceOf(RollbackException.class);
        assertThat(repository.findIncompletePublications()).isEmpty();
    }

    @Test
    void dropsEveryWriteOfATransactionMarkedRollbackOnlyWithoutSession() {
        // arrange
        UUID id = repository.create(publication("kept")).getIdentifier();
        List<Object> results = new ArrayList<>();

        // act
        Throwable thrown = catchThrowable(() -> QuarkusTransaction.requiringNew().run(() -> {
            QuarkusTransaction.setRollbackOnly();
            results.addAll(List.of(repository.claim(id, ELIGIBLE, T0, T1), repository.markProcessing(id),
                    repository.markStalePublicationsFailed(Status.PUBLISHED, T1),
                    repository.deletePublicationsCounting(List.of(id))));
        }));

        // assert
        assertThat(thrown).hasCauseInstanceOf(RollbackException.class);
        assertThat(results).containsExactly(false, false, 0, 0);
        assertThat(repository.findIncompletePublications()).singleElement()
                .satisfies(kept -> assertThat(kept.getStatus()).isEqualTo(Status.PUBLISHED))
                .satisfies(kept -> assertThat(kept.getCompletionAttempts()).isZero());
    }

    @Test
    void losesTheClaimToAConcurrentTransactionWithoutAWriteConflict() {
        // arrange
        UUID id = repository.create(publication("contended")).getIdentifier();

        // act
        List<Boolean> claims = QuarkusTransaction.requiringNew().call(() -> {
            boolean first = repository.claim(id, ELIGIBLE, T0, T1);
            boolean second = CompletableFuture.supplyAsync(() -> QuarkusTransaction.requiringNew()
                    .call(() -> repository.claim(id, ELIGIBLE, T0, T1)), otherThread).join();
            return List.of(first, second);
        });

        // assert
        assertThat(claims).containsExactly(true, false);
        assertThat(repository.findByStatus(Status.RESUBMITTED)).extracting(TargetEventPublication::getIdentifier)
                .containsExactly(id);
    }

    @Test
    void deliversAfterCommitOnlyOnceThePublicationIsVisible() {
        // arrange
        AtomicLong seen = new AtomicLong(-1);
        MongoCollection<Document> publications = client.getDatabase("shop").getCollection("event_publication");

        // act
        QuarkusTransaction.requiringNew().run(() -> {
            repository.create(publication("visible"));
            repository.afterCommit(() -> seen.set(publications.countDocuments()),
                    action -> registry.registerInterposedSynchronization(new RunAfterCommit(action)));
        });

        // assert
        assertThat(seen).hasValue(1);
    }

    @Test
    void commitsTheMongoTransactionBeforeDeliveringInsideTheTransaction() {
        // arrange
        AtomicLong seen = new AtomicLong(-1);
        List<Runnable> deliveries = new ArrayList<>();
        MongoCollection<Document> publications = client.getDatabase("shop").getCollection("event_publication");

        // act
        QuarkusTransaction.requiringNew().run(() -> {
            repository.create(publication("early"));
            repository.afterCommit(() -> seen.set(publications.countDocuments()), deliveries::add);
            deliveries.get(0).run();
        });

        // assert
        assertThat(seen).hasValue(1);
        assertThat(publications.countDocuments()).isEqualTo(1);
    }

    @Test
    void deliversRightAwayWhenTheMongoTransactionAlreadyCommitted() {
        // arrange
        AtomicLong seen = new AtomicLong(-1);
        List<Runnable> deliveries = new ArrayList<>();
        MongoCollection<Document> publications = client.getDatabase("shop").getCollection("event_publication");
        QuarkusTransaction.requiringNew().run(() -> {
            repository.create(publication("late"));
            repository.afterCommit(() -> seen.set(publications.countDocuments()), deliveries::add);
        });

        // act
        deliveries.get(0).run();

        // assert
        assertThat(seen).hasValue(1);
    }

    @Test
    void takesTheLatestAndEarliestInstantsAsBounds() {
        // arrange
        UUID id = repository.create(publication("bounded")).getIdentifier();
        repository.markProcessing(id);

        // act
        List<TargetEventPublication> beforeMax = repository.findIncompletePublicationsPublishedBefore(Instant.MAX);
        List<TargetEventPublication> beforeMin = repository.findIncompletePublicationsPublishedBefore(Instant.MIN);
        boolean claimed = repository.claim(id, Set.of(), Instant.MAX, T1);

        // assert
        assertThat(beforeMax).extracting(TargetEventPublication::getIdentifier).containsExactly(id);
        assertThat(beforeMin).isEmpty();
        assertThat(claimed).isTrue();
    }

    @Test
    void writesOnItsOwnOutsideTransactions() {
        // act
        repository.create(publication("alone"));

        // assert
        assertThat(sessions.current()).isEmpty();
        assertThat(repository.countByStatus(Status.PUBLISHED))
                .isEqualTo(1);
    }

    private static TargetEventPublication publication(String id) {
        return TargetEventPublication.of(new TckEvent(id), LISTENER, T0);
    }

    private static Parcel parcel(String label) {
        Parcel parcel = new Parcel();
        parcel.label = label;
        return parcel;
    }
}
