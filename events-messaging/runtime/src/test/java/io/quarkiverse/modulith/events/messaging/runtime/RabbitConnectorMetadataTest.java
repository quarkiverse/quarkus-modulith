package io.quarkiverse.modulith.events.messaging.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

class RabbitConnectorMetadataTest {

    @Test
    void usesTheKeyAsRoutingKeyAndLeavesTheExchangeToTheChannel() {
        // act
        OutgoingRabbitMQMetadata metadata = (OutgoingRabbitMQMetadata) new RabbitConnectorMetadata().create(
                new OutgoingRouting(RoutingTarget.forTarget("orders").andKey("o-1"), Map.of("tenant", "acme"), false,
                        Optional.of("application/json")));

        // assert
        assertThat(metadata.getRoutingKey()).isEqualTo("o-1");
        assertThat(metadata.getContentType()).isEqualTo("application/json");
        assertThat(metadata.getHeaders()).isEqualTo(Map.of("tenant", "acme"));
    }

    @Test
    void sendsTheTargetOfDynamicTargetsAsHeaders() {
        // act
        OutgoingRabbitMQMetadata metadata = (OutgoingRabbitMQMetadata) new RabbitConnectorMetadata().create(
                new OutgoingRouting(RoutingTarget.forTarget("eu-orders"), Map.of("tenant", "acme"), true,
                        Optional.empty()));

        // assert
        assertThat(metadata.getRoutingKey()).isNull();
        assertThat(metadata.getContentType()).isNull();
        assertThat(metadata.getHeaders()).isEqualTo(Map.of("tenant", "acme", "quarkusModulith_routingTarget",
                "eu-orders", "springModulith_routingTarget", "eu-orders"));
    }
}
