package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderAudited;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderRerouted;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.RecordingBroker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

class NoDefaultChannelTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String MESSAGE = "No Quarkus Messaging channel for the routing target eu-orders: add it to"
            + " quarkus.modulith.events.messaging.channels, or set quarkus.modulith.events.messaging.default-channel";

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recordingStaticTargets(ShopApplication.create())
            .setLogRecordPredicate(record -> record.getThrown() instanceof IllegalStateException)
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(record -> assertThat(record.getThrown()).hasMessage(MESSAGE)));

    @Inject
    OrderService orders;

    @Inject
    RecordingBroker broker;

    @Inject
    EventPublicationRepository repository;

    @Test
    void failsTheExternalizationOfATargetWithoutChannel() {
        // act
        orders.reroute(new OrderRerouted("eu", "o-1"));

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).size() == 1);
        assertThat(repository.findIncompletePublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getEvent()).isEqualTo(new OrderRerouted("eu", "o-1")));
    }

    @Test
    void routesByTheApplicationLocalNameWithTheDefaultConfiguration() {
        // act
        orders.audit(new OrderAudited("o-2"));

        // assert
        await().atMost(TIMEOUT).until(() -> broker.received("order.OrderAudited").size() == 1);
        assertThat(broker.received("order.OrderAudited").get(0).getPayload()).isEqualTo("{\"id\":\"o-2\"}");
    }
}
