package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.externalization.runtime.EventExternalizerModuleListener;
import io.quarkiverse.modulith.events.jpa.deployment.externalized.CapturingTransport;
import io.quarkiverse.modulith.events.jpa.deployment.externalized.ShipmentService;
import io.quarkus.test.QuarkusExtensionTest;

public class ExternalizationWithoutTransactionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(ShipmentService.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.modulith.base-package", ShipmentService.class.getPackageName())
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    ShipmentService shipments;

    @Inject
    CapturingTransport transport;

    @Inject
    CompletedEventPublications completed;

    @Test
    void storesThePublicationInATransactionOfItsOwn() {
        // act
        shipments.dispatch("s-1");

        // assert
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(completed.findAll())
                .extracting(EventPublication::getListenerId)
                .containsExactly(EventExternalizerModuleListener.LISTENER_ID));
        assertThat(transport.targets).containsExactly("shipments::s-1");
    }
}
