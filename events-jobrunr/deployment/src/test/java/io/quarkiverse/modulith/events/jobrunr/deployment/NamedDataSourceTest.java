package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingBrokerTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;

class NamedDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", RecordingBrokerTransport.class,
            SentMessage.class)
            .overrideConfigKey("quarkus.datasource.jobs.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jobs.jdbc.url", "jdbc:h2:mem:named-jobs;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.jobrunr.database.datasource", "jobs");

    @Inject
    OrderService orders;

    @Inject
    RecordingBrokerTransport brokerTransport;

    @Test
    void sendsThroughTheBrokerFromJobsStoredInTheNamedDataSource() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", List.of("pen"));

        // act
        orders.place(event);

        // assert
        await().atMost(Duration.ofSeconds(40)).until(() -> brokerTransport.sent()
                .equals(List.of(new SentMessage(event, RoutingTarget.forTarget("orders")))));
    }
}
