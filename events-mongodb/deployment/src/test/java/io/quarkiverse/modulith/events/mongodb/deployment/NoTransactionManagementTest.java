package io.quarkiverse.modulith.events.mongodb.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoSessions;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class NoTransactionManagementTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TckEvent.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.devservices.image-name", "mongo:7.0.48")
            .overrideConfigKey("quarkus.modulith.events.mongodb.transaction-management.enabled", "false");

    @Inject
    EventPublicationRepository repository;

    @Inject
    MongoSessions sessions;

    @Inject
    MongoStore store;

    @Test
    void deliversAfterTheJtaCommitWithoutSession() {
        // arrange
        Runnable deliveries = () -> {
        };
        List<Runnable> scheduled = new ArrayList<>();

        // act
        repository.afterCommit(deliveries, scheduled::add);

        // assert
        assertThat(scheduled).containsExactly(deliveries);
    }

    @Test
    void keepsPublicationsOfRolledBackTransactions() {
        // arrange
        store.database().drop();

        // act
        Throwable thrown = catchThrowable(() -> QuarkusTransaction.requiringNew().run(() -> {
            repository.create(TargetEventPublication.of(new TckEvent("kept"),
                    PublicationTargetIdentifier.of("billing.on-order"), Instant.parse("2026-01-01T10:00:00Z")));
            throw new IllegalStateException("rolled back");
        }));

        // assert
        assertThat(thrown).hasMessage("rolled back");
        assertThat(repository.findIncompletePublications()).extracting(TargetEventPublication::getEvent)
                .containsExactly(new TckEvent("kept"));
    }

    @Test
    void usesTheDefaultDatabaseWithoutSessions() {
        // act
        String database = store.database().getName();

        // assert
        assertThat(sessions).isInstanceOf(NoMongoSessions.class);
        assertThat(database).isEqualTo("modulith");
    }
}
