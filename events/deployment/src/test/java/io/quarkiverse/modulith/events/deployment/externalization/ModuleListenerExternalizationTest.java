package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.RoutingTarget;
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
import io.quarkiverse.modulith.events.externalization.runtime.EventExternalizerModuleListener;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkus.test.QuarkusExtensionTest;

class ModuleListenerExternalizationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class, Repositories.class,
                            ExternalizedEvents.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    RecordingTransport transport;

    @Inject
    EventPublicationRepository repository;

    @Inject
    ExternalizedEvents externalized;

    @Inject
    EventRedelivery redelivery;

    @BeforeEach
    void reset() {
        transport.reset();
        externalized.reset();
        repository.deletePublications(repository.findIncompletePublications().stream()
                .map(TargetEventPublication::getIdentifier).toList());
        repository.deleteCompletedPublications();
    }

    private List<TargetEventPublication> completed(Object event) {
        return repository.findCompletedPublications().stream()
                .filter(publication -> publication.getEvent().equals(event)).toList();
    }

    @Test
    void sendsAfterCommitAndCompletesThePublication() {
        // act
        orders.placeInTransaction(new OrderPlaced("o-1", 3));

        // assert
        await().atMost(TIMEOUT).until(() -> completed(new OrderPlaced("o-1", 3)).size() == 1);
        assertThat(transport.sent()).containsExactly(new SentMessage(new OrderPlaced("o-1", 3),
                RoutingTarget.forTarget("orders").andKey("o-1"), Map.of()));
        TargetEventPublication publication = completed(new OrderPlaced("o-1", 3)).get(0);
        assertThat(publication.getListenerId()).isEqualTo(EventExternalizerModuleListener.LISTENER_ID);
        assertThat(publication.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(publication.getCompletionAttempts()).isEqualTo(1);
        await().atMost(TIMEOUT).until(() -> externalized.received().size() == 1);
        assertThat(externalized.received().get(0).getBrokerResult()).isEqualTo("ack-1");
        assertThat(externalized.received().get(0).getTarget()).hasToString("orders::o-1");
    }

    @Test
    void routesByTheApplicationLocalNameWithoutTransaction() {
        // act
        orders.cancel(new OrderCancelled("o-2"));

        // assert
        await().atMost(TIMEOUT).until(() -> transport.sent().size() == 1);
        assertThat(transport.sent().get(0).target()).isEqualTo(RoutingTarget.forTarget("order.OrderCancelled"));
    }

    @Test
    void sendsNothingWhenTheTransactionRollsBack() {
        // act
        Throwable thrown = catchThrowable(() -> orders.placeAndRollBack(new OrderPlaced("o-3", 1)));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("rolled back on purpose");
        assertThat(transport.sent()).isEmpty();
        assertThat(completed(new OrderPlaced("o-3", 1))).isEmpty();
    }

    @Test
    void ignoresEventsThatAreNotExternalized() {
        // act
        orders.note(new OrderNoted("o-4"));
        orders.cancel(new OrderCancelled("o-5"));

        // assert
        await().atMost(TIMEOUT).until(() -> transport.sent().size() == 1);
        assertThat(transport.sent()).extracting(SentMessage::payload).containsExactly(new OrderCancelled("o-5"));
    }

    @Test
    void leavesThePublicationFailedWhenTheBrokerIsDown() {
        // arrange
        transport.down(true);

        // act
        orders.placeInTransaction(new OrderPlaced("o-6", 2));

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).size() == 1);
        assertThat(repository.findIncompletePublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getEvent()).isEqualTo(new OrderPlaced("o-6", 2)));
        assertThat(externalized.received()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { EventExternalizerModuleListener.LISTENER_ID,
            EventExternalizerModuleListener.SPRING_LISTENER_ID })
    void resubmitsThroughTheRegistrySpi(String listenerId) {
        // arrange
        TargetEventPublication stored = repository.create(TargetEventPublication.of(new OrderPlaced("o-7", 1),
                PublicationTargetIdentifier.of(listenerId), Instant.now()));

        // act
        boolean resubmitted = redelivery.redeliver(stored.getIdentifier(), listenerId, stored.getEvent());
        boolean unknown = redelivery.redeliver(UUID.randomUUID(), "com.acme.Listener.on(Event)", "event");

        // assert
        assertThat(resubmitted).isTrue();
        assertThat(unknown).isFalse();
        await().atMost(TIMEOUT).until(() -> completed(new OrderPlaced("o-7", 1)).size() == 1);
        assertThat(transport.sent()).extracting(SentMessage::payload).containsExactly(new OrderPlaced("o-7", 1));
    }
}
