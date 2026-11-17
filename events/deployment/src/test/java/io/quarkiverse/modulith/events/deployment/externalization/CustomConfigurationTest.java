package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.custom.CustomConfiguration;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderNoted;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;

class CustomConfigurationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class, CustomConfiguration.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    RecordingTransport transport;

    @Test
    void selectsMapsAndRoutesWithTheApplicationConfiguration() {
        // act
        orders.place(new OrderPlaced("o-1", 1));
        orders.note(new OrderNoted("note-7"));
        orders.note(new OrderNoted("n-8"));

        // assert
        await().atMost(Duration.ofSeconds(5)).until(() -> transport.sent().size() == 2);
        assertThat(transport.sent()).containsExactlyInAnyOrder(
                new SentMessage("note-7", RoutingTarget.forTarget("notes").andKey("note-7"), Map.of("length", 6)),
                new SentMessage("n-8", RoutingTarget.forTarget("notes").andKey("n-8"), Map.of("length", 3)));
    }
}
