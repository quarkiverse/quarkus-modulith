package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.ExternalizedEvents;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.Repositories;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

@VirtualThreadUnit
@ShouldNotPin
class VirtualThreadSendTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class, Repositories.class,
                            ExternalizedEvents.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.listeners.executor", "virtual-threads");

    @Inject
    OrderService orders;

    @Inject
    RecordingTransport transport;

    @Inject
    EventPublicationRepository repository;

    @Test
    void sendsOnVirtualThreadsWithoutPinning() {
        // act
        orders.placeInTransaction(new OrderPlaced("o-vt", 1));

        // assert
        await().atMost(Duration.ofSeconds(5)).until(() -> repository.findCompletedPublications().size() == 1);
        assertThat(transport.sent()).containsExactly(new SentMessage(new OrderPlaced("o-vt", 1),
                RoutingTarget.forTarget("orders").andKey("o-vt"), Map.of()));
        assertThat(transport.threads()).containsExactly(true);
    }
}
