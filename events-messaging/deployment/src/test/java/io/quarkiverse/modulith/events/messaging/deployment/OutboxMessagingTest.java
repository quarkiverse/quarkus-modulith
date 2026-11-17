package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.BrokerMode;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.RecordingBroker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

class OutboxMessagingTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recordingStaticTargets(ShopApplication.create())
            .overrideConfigKey("quarkus.modulith.events.externalization.mode", "outbox")
            .overrideConfigKey("quarkus.modulith.events.externalization.outbox.interval", "200ms");

    @Inject
    OrderService orders;

    @Inject
    RecordingBroker broker;

    @Inject
    EventPublicationRepository repository;

    @Test
    void keepsThePublicationWhileTheBrokerRejectsAndSendsItOnceTheBrokerIsBack() {
        // arrange
        broker.mode(BrokerMode.REJECT);
        orders.place(new OrderPlaced("o-1", "acme"));
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).size() == 1);

        // act
        broker.mode(BrokerMode.ACKNOWLEDGE);

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 1);
        assertThat(repository.findIncompletePublications()).isEmpty();
        assertThat(broker.received("orders")).hasSizeGreaterThan(1).extracting(message -> (Object) message.getPayload())
                .containsOnly("{\"id\":\"o-1\",\"tenant\":\"acme\"}");
    }
}
