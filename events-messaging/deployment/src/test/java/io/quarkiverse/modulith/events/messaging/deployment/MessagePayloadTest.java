package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.messaging.RoutingMetadata;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.AckTracker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.MessageMapping;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.RecordingBroker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

class MessagePayloadTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication
            .recordingStaticTargets(ShopApplication.create(MessageMapping.class, AckTracker.class));

    @Inject
    OrderService orders;

    @Inject
    RecordingBroker broker;

    @Inject
    AckTracker tracker;

    @Inject
    EventPublicationRepository repository;

    @Test
    void sendsAMappedMessageAsItIsAndAddsOnlyTheMetadataItLacks() {
        // act
        orders.place(new OrderPlaced("o-1", "acme"));

        // assert
        await().atMost(TIMEOUT).until(() -> broker.received("orders").size() == 1);
        Message<?> message = broker.received("orders").get(0);
        assertThat(message.getPayload()).isEqualTo("o-1");
        assertThat(message.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow().getKey()).isEqualTo("custom");
        assertThat(message.getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow().getRoutingKey()).isEqualTo("o-1");
        assertThat(message.getMetadata(RoutingMetadata.class).orElseThrow().target())
                .isEqualTo(RoutingTarget.forTarget("orders").andKey("o-1"));
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 1);
        assertThat(tracker.acknowledged()).isEqualTo(1);
    }
}
