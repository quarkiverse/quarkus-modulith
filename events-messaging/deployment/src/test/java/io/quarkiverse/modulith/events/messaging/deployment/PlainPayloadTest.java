package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.RecordingBroker;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;

class PlainPayloadTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recordingStaticTargets(ShopApplication.create())
            .overrideConfigKey("quarkus.modulith.events.messaging.enable-json", "false");

    @Inject
    OrderService orders;

    @Inject
    RecordingBroker broker;

    @Test
    void sendsTheEventAsItIsWithoutContentType() {
        // act
        orders.place(new OrderPlaced("o-1", "acme"));

        // assert
        await().atMost(Duration.ofSeconds(10)).until(() -> broker.received("orders").size() == 1);
        Message<?> message = broker.received("orders").get(0);
        assertThat(message.getPayload()).isEqualTo(new OrderPlaced("o-1", "acme"));
        assertThat(message.getMetadata(OutgoingAmqpMetadata.class).orElseThrow().getContentType()).isNull();
    }
}
