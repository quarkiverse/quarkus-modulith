package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ThrottledBroker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ThrottledConnector;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ThrottledSubscriber;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The broker requests one message and leaves it unacknowledged, and the emitter buffers two more, so the fourth event
 * overflows the channel.
 */
class EmitterBufferTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recordingStaticTargets(ShopApplication.create(
            ThrottledConnector.class, ThrottledSubscriber.class, ThrottledBroker.class))
            .overrideConfigKey("mp.messaging.outgoing.orders.connector", ThrottledConnector.NAME)
            .overrideConfigKey("mp.messaging.emitter.default-buffer-size", "2");

    @Inject
    OrderService orders;

    @Inject
    ThrottledBroker broker;

    @Inject
    EventPublicationRepository repository;

    @Test
    void failsThePublicationThatOverflowsTheChannelAndTakesMessagesAgainOnceTheBrokerRequestsMore() {
        // arrange
        Stream.of("o-1", "o-2", "o-3", "o-4").forEach(id -> orders.place(new OrderPlaced(id, "acme")));
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).size() == 1);

        // act
        broker.release();
        orders.place(new OrderPlaced("o-5", "acme"));

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 4);
        assertThat(repository.findByStatus(Status.FAILED)).hasSize(1);
        assertThat(broker.received()).extracting(message -> (Object) message.getPayload()).hasSize(4)
                .contains("{\"id\":\"o-5\",\"tenant\":\"acme\"}");
        assertThat(broker.failures()).isEmpty();
    }
}
