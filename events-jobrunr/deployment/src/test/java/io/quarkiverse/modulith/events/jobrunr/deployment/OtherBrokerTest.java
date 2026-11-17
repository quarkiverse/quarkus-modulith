package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.deployment.externalization.ExternalizationTransportBuildItem;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingBrokerTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingJobRunrTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;

class OtherBrokerTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", RecordingJobRunrTransport.class,
            RecordingBrokerTransport.class, SentMessage.class)
            .addBuildChainCustomizer(chain -> chain
                    .addBuildStep(context -> context.produce(new ExternalizationTransportBuildItem("other-extension")))
                    .produces(ExternalizationTransportBuildItem.class).build());

    @Inject
    OrderService orders;

    @Inject
    RecordingBrokerTransport brokerTransport;

    @Test
    void sendsThroughTheBrokerOfAnotherExtension() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", List.of());

        // act
        orders.place(event);

        // assert
        await().atMost(Duration.ofSeconds(40)).until(() -> brokerTransport.sent()
                .equals(List.of(new SentMessage(event, RoutingTarget.forTarget("orders")))));
    }
}
