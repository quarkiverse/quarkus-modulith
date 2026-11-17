package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.RecordingConnector;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkiverse.modulith.events.messaging.runtime.ChannelProperties;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The connector of {@code shipments} is set at runtime only, so the build applies the Kafka and RabbitMQ defaults to
 * it, while {@code orders} declares the recording connector at build time.
 */
class ChannelDefaultsBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recording(ShopApplication.create(), "orders",
            "order.OrderCancelled", "order.OrderAudited")
            .overrideRuntimeConfigKey(ChannelProperties.outgoing("shipments", "connector"), RecordingConnector.NAME);

    @Test
    void setsTheDefaultsOnlyOnChannelsWithoutAKnownConnector() {
        // act
        Config config = ConfigProvider.getConfig();

        // assert
        assertThat(config.getValue("mp.messaging.outgoing.shipments.acks", String.class)).isEqualTo("all");
        assertThat(config.getValue("mp.messaging.outgoing.shipments.enable.idempotence", Boolean.class)).isTrue();
        assertThat(config.getValue("mp.messaging.outgoing.shipments.value.serializer", String.class))
                .isEqualTo("org.apache.kafka.common.serialization.StringSerializer");
        assertThat(config.getValue("mp.messaging.outgoing.shipments.publish-confirms", Boolean.class)).isTrue();
        assertThat(config.getOptionalValue("mp.messaging.outgoing.orders.acks", String.class)).isEmpty();
        assertThat(config.getOptionalValue("mp.messaging.outgoing.orders.publish-confirms", Boolean.class)).isEmpty();
    }
}
