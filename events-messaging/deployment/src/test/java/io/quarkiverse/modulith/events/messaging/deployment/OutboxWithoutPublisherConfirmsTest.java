package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

class OutboxWithoutPublisherConfirmsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recordingStaticTargets(ShopApplication.create())
            .overrideConfigKey("quarkus.modulith.events.externalization.mode", "outbox")
            .overrideConfigKey("mp.messaging.outgoing.orders.publish-confirms", "false")
            .overrideConfigKey("mp.messaging.outgoing.shipments.publish-confirms", "false")
            .assertException(failure -> assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessage("The outbox externalization mode"
                            + " needs RabbitMQ publisher confirms, but they are off for the channels [orders, shipments]:"
                            + " remove mp.messaging.outgoing.orders.publish-confirms=false, or set"
                            + " quarkus.modulith.events.externalization.mode=module-listener"));

    @Test
    void failsTheStartup() {
    }
}
