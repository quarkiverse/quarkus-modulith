package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.ProducedTransports;
import io.quarkus.test.QuarkusExtensionTest;

class JobRunrOnlyTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", ProducedTransports.class);

    @Inject
    OrderService orders;

    @Inject
    ProducedTransports transports;

    @Inject
    EventExternalizationTransport brokerTransport;

    @Test
    void externalizesThroughTheApplicationTransportWithoutBroker() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", List.of("book"));

        // act
        orders.place(event);

        // assert
        await().atMost(Duration.ofSeconds(40)).until(() -> transports.delivered().equals(List.of(event)));
    }

    @Test
    void refusesToSendThroughTheMissingBroker() {
        // act
        CompletableFuture<?> sent = brokerTransport.externalize("o-2", RoutingTarget.forTarget("orders"), Map.of())
                .toCompletableFuture();

        // assert
        assertThat(sent.exceptionNow()).isInstanceOf(IllegalStateException.class)
                .hasMessage("No broker transport is present: events are externalized through the"
                        + " JobRunrExternalizationTransport beans of the application only. Add a broker transport such as"
                        + " quarkus-modulith-events-jms to send events through EventExternalizer");
    }
}
