package io.quarkiverse.modulith.events.jms.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.jms.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jms.deployment.fixtures.support.ApplicationTransport;
import io.quarkus.test.QuarkusExtensionTest;

class ApplicationTransportTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addClasses(OrderPlaced.class, ApplicationTransport.class))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.events.jms.deployment.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    EventExternalizationTransport transport;

    @Test
    void anApplicationTransportReplacesTheJmsTransportWithoutConnectionFactory() {
        // act
        Class<?> type = transport.getClass();

        // assert
        assertThat(type).isEqualTo(ApplicationTransport.class);
    }
}
