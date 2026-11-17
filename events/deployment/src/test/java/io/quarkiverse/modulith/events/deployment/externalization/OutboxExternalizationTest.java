package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderCancelled;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderNoted;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.ExternalizedEvents;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.Repositories;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkiverse.modulith.events.externalization.runtime.OutboxEventExternalizer;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkus.test.QuarkusExtensionTest;

class OutboxExternalizationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class, Repositories.class,
                            ExternalizedEvents.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.events.externalization.mode", "outbox")
            .overrideConfigKey("quarkus.modulith.events.externalization.outbox.interval", "200ms")
            .overrideConfigKey("quarkus.modulith.events.externalization.outbox.send-timeout", "300ms")
            .overrideConfigKey("quarkus.modulith.events.externalization.outbox.stale-after", "100ms");

    @Inject
    OrderService orders;

    @Inject
    RecordingTransport transport;

    @Inject
    EventPublicationRepository repository;

    @Inject
    EventRedelivery redelivery;

    @BeforeEach
    void reset() {
        transport.reset();
        await().atMost(TIMEOUT).until(() -> repository.findIncompletePublications().isEmpty());
        repository.deleteCompletedPublications();
    }

    private Optional<TargetEventPublication> publicationOf(Object event) {
        return Stream.concat(repository.findIncompletePublications().stream(),
                repository.findCompletedPublications().stream())
                .filter(publication -> publication.getEvent().equals(event)).findFirst();
    }

    @Test
    void sendsRecordedEventsInOrderAfterTheCommit() {
        // act
        orders.placeInTransaction(new OrderPlaced("o-1", 1));
        orders.placeInTransaction(new OrderPlaced("o-2", 2));
        orders.cancel(new OrderCancelled("o-1"));

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 3);
        assertThat(transport.sent()).extracting(SentMessage::payload).containsExactly(new OrderPlaced("o-1", 1),
                new OrderPlaced("o-2", 2), new OrderCancelled("o-1"));
        assertThat(repository.findCompletedPublications()).extracting(TargetEventPublication::getListenerId)
                .containsOnly(OutboxEventExternalizer.LISTENER_ID);
    }

    @Test
    void keepsTheOrderWhileTheBrokerIsDownAndCatchesUpAfterwards() {
        // arrange
        transport.down(true);

        // act
        orders.placeInTransaction(new OrderPlaced("o-3", 1));
        orders.placeInTransaction(new OrderPlaced("o-4", 1));

        // assert
        await().atMost(TIMEOUT).until(() -> publicationOf(new OrderPlaced("o-3", 1))
                .filter(publication -> publication.getCompletionAttempts() >= 2).isPresent());
        assertThat(publicationOf(new OrderPlaced("o-4", 1))).get()
                .satisfies(publication -> assertThat(publication.getStatus()).isEqualTo(Status.PUBLISHED))
                .satisfies(publication -> assertThat(publication.getCompletionAttempts()).isZero());
        transport.down(false);
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 2);
        assertThat(transport.sent()).extracting(SentMessage::payload).containsExactly(new OrderPlaced("o-3", 1),
                new OrderPlaced("o-4", 1));
    }

    @Test
    void failsSendsTheBrokerDoesNotAcknowledgeInTime() {
        // arrange
        transport.hang(true);

        // act
        orders.placeInTransaction(new OrderPlaced("o-5", 1));

        // assert
        await().atMost(TIMEOUT).until(() -> publicationOf(new OrderPlaced("o-5", 1))
                .filter(publication -> publication.getCompletionAttempts() >= 2).isPresent());
        transport.hang(false);
        await().atMost(TIMEOUT).until(() -> publicationOf(new OrderPlaced("o-5", 1))
                .filter(TargetEventPublication::isCompleted).isPresent());
    }

    @Test
    void sendsPublicationsTheRegistryResubmits() {
        // arrange
        TargetEventPublication claimed = repository.create(TargetEventPublication.of(new OrderPlaced("o-6", 1),
                PublicationTargetIdentifier.of(OutboxEventExternalizer.LISTENER_ID), Instant.now()));
        repository.markResubmitted(claimed.getIdentifier(), Instant.now());

        // act
        boolean resubmitted = redelivery.redeliver(claimed.getIdentifier(), OutboxEventExternalizer.LISTENER_ID,
                claimed.getEvent());
        boolean unknown = redelivery.redeliver(UUID.randomUUID(), "com.acme.Listener.on(Event)", "event");

        // assert
        assertThat(resubmitted).isTrue();
        assertThat(unknown).isFalse();
        await().atMost(TIMEOUT).until(() -> publicationOf(new OrderPlaced("o-6", 1))
                .filter(TargetEventPublication::isCompleted).isPresent());
        assertThat(transport.sent()).extracting(SentMessage::payload).containsExactly(new OrderPlaced("o-6", 1));
    }

    @Test
    void storesNothingForEventsThatAreNotExternalized() {
        // act
        orders.note(new OrderNoted("x"));

        // assert
        assertThat(repository.findIncompletePublications()).isEmpty();
        assertThat(transport.sent()).isEmpty();
    }
}
