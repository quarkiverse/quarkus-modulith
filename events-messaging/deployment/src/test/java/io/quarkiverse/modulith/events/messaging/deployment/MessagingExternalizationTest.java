package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventExternalized;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.messaging.RoutingMetadata;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderAudited;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderCancelled;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderRerouted;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderShipped;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.BrokerMode;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ExternalizedEvents;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.RecordingBroker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopExternalization;
import io.quarkiverse.modulith.events.messaging.impl.MessagingChannels;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

class MessagingExternalizationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recording(ShopApplication.recordingStaticTargets(
            ShopApplication.create(ShopExternalization.class)), "audit", "fallback")
            .overrideConfigKey("quarkus.modulith.events.messaging.channels", "audit")
            .overrideConfigKey("quarkus.modulith.events.messaging.default-channel", "fallback")
            .overrideConfigKey("quarkus.modulith.events.messaging.ack-timeout", "500ms");

    @Inject
    OrderService orders;

    @Inject
    RecordingBroker broker;

    @Inject
    EventPublicationRepository repository;

    @Inject
    ExternalizedEvents externalized;

    @Inject
    MessagingChannels channels;

    @Inject
    EventRedelivery redelivery;

    @BeforeEach
    void reset() {
        broker.reset();
        externalized.reset();
        repository.deletePublications(repository.findIncompletePublications().stream()
                .map(TargetEventPublication::getIdentifier).toList());
        repository.deleteCompletedPublications();
    }

    private Message<?> receivedOne(String channel) {
        await().atMost(TIMEOUT).until(() -> broker.received(channel).size() == 1);
        return broker.received(channel).get(0);
    }

    @Test
    void generatesOneChannelPerStaticTargetConfiguredChannelAndDefaultChannel() {
        // act
        var names = channels.names();

        // assert
        assertThat(names).containsExactly("audit", "fallback", "order.OrderAudited",
                "order.OrderCancelled", "orders", "shipments");
    }

    @Test
    void sendsJsonToTheChannelOfTheTargetWithKeyAndHeaders() {
        // act
        orders.place(new OrderPlaced("o-1", "acme"));

        // assert
        Message<?> message = receivedOne("orders");
        assertThat(message.getPayload()).isEqualTo("{\"id\":\"o-1\",\"tenant\":\"acme\"}");
        assertThat(message.getMetadata(RoutingMetadata.class)).hasValue(new RoutingMetadata(
                RoutingTarget.forTarget("orders").andKey("o-1"), Map.of("tenant", "acme",
                        RoutingMetadata.ROUTING_TARGET_HEADER, "orders::o-1",
                        RoutingMetadata.SPRING_ROUTING_TARGET_HEADER, "orders::o-1")));
        OutgoingKafkaRecordMetadata<?> kafka = message.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow();
        assertThat(kafka.getKey()).isEqualTo("o-1");
        assertThat(kafka.getTopic()).isNull();
        assertThat(new String(kafka.getHeaders().lastHeader("tenant").value(), StandardCharsets.UTF_8))
                .isEqualTo("acme");
        assertThat(message.getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow().getRoutingKey()).isEqualTo("o-1");
        OutgoingAmqpMetadata amqp = message.getMetadata(OutgoingAmqpMetadata.class).orElseThrow();
        assertThat(amqp.getSubject()).isEqualTo("o-1");
        assertThat(amqp.getAddress()).isNull();
        assertThat(amqp.getContentType()).isEqualTo("application/json");
        assertThat(amqp.getProperties().getString("tenant")).isEqualTo("acme");
    }

    @Test
    void sendsAnEventWithoutTheHeadersWhoseValueIsNull() {
        // act
        orders.place(new OrderPlaced("o-9", null));

        // assert
        Message<?> message = receivedOne("orders");
        assertThat(message.getMetadata(RoutingMetadata.class).orElseThrow().headers()).isEqualTo(Map.of(
                RoutingMetadata.ROUTING_TARGET_HEADER, "orders::o-9",
                RoutingMetadata.SPRING_ROUTING_TARGET_HEADER, "orders::o-9"));
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 1);
    }

    @Test
    void completesThePublicationAndFiresEventExternalizedOnAcknowledgement() {
        // act
        orders.place(new OrderPlaced("o-2", "acme"));

        // assert
        await().atMost(TIMEOUT).until(() -> externalized.received().size() == 1);
        EventExternalized<?, ?> event = externalized.received().get(0);
        assertThat(event.getEvent()).isEqualTo(new OrderPlaced("o-2", "acme"));
        assertThat(event.getTarget()).isEqualTo(RoutingTarget.forTarget("orders").andKey("o-2"));
        assertThat(event.getBrokerResult()).isEqualTo(RecordingBroker.RESULT);
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 1);
        TargetEventPublication publication = repository.findCompletedPublications().get(0);
        assertThat(publication.getEvent()).isEqualTo(new OrderPlaced("o-2", "acme"));
        assertThat(publication.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void routesSpringKeyExpressionsAndTheApplicationLocalName() {
        // act
        orders.ship(new OrderShipped("o-3"));
        orders.cancel(new OrderCancelled("o-4"));

        // assert
        Message<?> shipped = receivedOne("shipments");
        assertThat(shipped.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow().getKey()).isEqualTo("o-3");
        Message<?> cancelled = receivedOne("order.OrderCancelled");
        assertThat(cancelled.getPayload()).isEqualTo("{\"id\":\"o-4\"}");
        assertThat(cancelled.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow().getKey()).isNull();
        assertThat(cancelled.getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow().getRoutingKey()).isNull();
        assertThat(cancelled.getMetadata(OutgoingAmqpMetadata.class).orElseThrow().getSubject()).isNull();
    }

    @Test
    void sendsToAConfiguredChannelForTargetsOfACustomConfiguration() {
        // act
        orders.audit(new OrderAudited("o-5"));

        // assert
        Message<?> message = receivedOne("audit");
        assertThat(message.getMetadata(RoutingMetadata.class).orElseThrow().target())
                .isEqualTo(RoutingTarget.forTarget("audit"));
        assertThat(broker.received("order.OrderAudited")).isEmpty();
    }

    @Test
    void sendsExpressionTargetsThroughTheDefaultChannelAddressedPerMessage() {
        // act
        orders.reroute(new OrderRerouted("eu", "o-6"));

        // assert
        Message<?> message = receivedOne("fallback");
        assertThat(message.getMetadata(RoutingMetadata.class).orElseThrow().target())
                .isEqualTo(RoutingTarget.forTarget("eu-orders").andKey("o-6"));
        assertThat(message.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow().getTopic())
                .isEqualTo("eu-orders");
        assertThat(message.getMetadata(OutgoingAmqpMetadata.class).orElseThrow().getAddress()).isEqualTo("eu-orders");
    }

    @Test
    void leavesThePublicationFailedWhileTheBrokerRejectsAndCompletesItWhenRedelivered() {
        // arrange
        broker.mode(BrokerMode.REJECT);

        // act
        orders.place(new OrderPlaced("o-7", "acme"));
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).size() == 1);
        broker.mode(BrokerMode.ACKNOWLEDGE);
        TargetEventPublication failed = repository.findIncompletePublications().get(0);
        boolean redelivered = redelivery.redeliver(failed.getIdentifier(), failed.getListenerId(), failed.getEvent());

        // assert
        assertThat(redelivered).isTrue();
        await().atMost(TIMEOUT).until(() -> repository.findCompletedPublications().size() == 1);
        assertThat(repository.findIncompletePublications()).isEmpty();
        assertThat(broker.received("orders")).hasSize(2);
        assertThat(externalized.received()).hasSize(1);
    }

    @Test
    void failsThePublicationWhenTheBrokerDoesNotAcknowledgeInTime() {
        // arrange
        broker.mode(BrokerMode.SILENT);

        // act
        orders.place(new OrderPlaced("o-8", "acme"));

        // assert
        await().atMost(TIMEOUT).until(() -> repository.findByStatus(Status.FAILED).size() == 1);
        assertThat(broker.received("orders")).hasSize(1);
        assertThat(externalized.received()).isEmpty();
    }
}
