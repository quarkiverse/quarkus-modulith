package io.quarkiverse.modulith.events.messaging.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import io.vertx.core.json.JsonObject;

class AmqpConnectorMetadataTest {

    @Test
    void usesTheKeyAsSubjectAndTheHeadersAsApplicationProperties() {
        // act
        OutgoingAmqpMetadata metadata = (OutgoingAmqpMetadata) new AmqpConnectorMetadata().create(new OutgoingRouting(
                RoutingTarget.forTarget("orders").andKey("o-1"), Map.of("tenant", "acme", "count", 2), false,
                Optional.of("application/json")));

        // assert
        assertThat(metadata.getSubject()).isEqualTo("o-1");
        assertThat(metadata.getAddress()).isNull();
        assertThat(metadata.getContentType()).isEqualTo("application/json");
        assertThat(metadata.getProperties()).isEqualTo(new JsonObject().put("tenant", "acme").put("count", 2));
    }

    @Test
    void setsTheAddressOfDynamicTargetsWithoutSubjectOrContentType() {
        // act
        OutgoingAmqpMetadata metadata = (OutgoingAmqpMetadata) new AmqpConnectorMetadata().create(new OutgoingRouting(
                RoutingTarget.forTarget("eu-orders"), Map.of(), true, Optional.empty()));

        // assert
        assertThat(metadata.getAddress()).isEqualTo("eu-orders");
        assertThat(metadata.getSubject()).isNull();
        assertThat(metadata.getContentType()).isNull();
        assertThat(metadata.getProperties()).isEqualTo(new JsonObject());
    }
}
