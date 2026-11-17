package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;

class WithoutRepositoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    RecordingTransport transport;

    @Test
    void sendsWithoutStoringPublications() {
        // act
        orders.placeInTransaction(new OrderPlaced("o-1", 1));

        // assert
        await().atMost(Duration.ofSeconds(5)).until(() -> transport.sent().size() == 1);
        assertThat(transport.sent()).extracting(SentMessage::payload).containsExactly(new OrderPlaced("o-1", 1));
    }
}
