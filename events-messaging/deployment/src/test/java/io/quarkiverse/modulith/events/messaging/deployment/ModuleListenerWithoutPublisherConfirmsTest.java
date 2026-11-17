package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkiverse.modulith.events.messaging.runtime.RabbitConfirmsWarning;
import io.quarkus.test.QuarkusExtensionTest;

class ModuleListenerWithoutPublisherConfirmsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.recordingStaticTargets(ShopApplication.create())
            .overrideConfigKey("mp.messaging.outgoing.orders.publish-confirms", "false")
            .overrideConfigKey("mp.messaging.outgoing.shipments.publish-confirms", "false")
            .setLogRecordPredicate(r -> RabbitConfirmsWarning.class.getName().equals(r.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).singleElement().satisfies(r -> {
                assertThat(r.getLevel()).isEqualTo(Level.WARNING);
                assertThat(r.getParameters()).containsExactly(List.of("orders", "shipments"),
                        "mp.messaging.outgoing.orders.publish-confirms");
            }));

    @Test
    void warnsAtStartup() {
    }
}
