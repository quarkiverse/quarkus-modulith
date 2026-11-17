package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingJobRunrTransport;
import io.quarkiverse.modulith.events.jobrunr.runtime.TransactionalStorageProviderProducer;
import io.quarkus.test.QuarkusExtensionTest;

class InMemoryStorageTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", RecordingJobRunrTransport.class)
            .overrideConfigKey("quarkus.jobrunr.database.type", "mem");

    @Inject
    OrderService orders;

    @Inject
    RecordingJobRunrTransport transport;

    @Inject
    Instance<TransactionalStorageProviderProducer> storage;

    @Test
    void keepsJobRunrsOwnStorageWhenItIsNotSql() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", List.of());

        // act
        orders.place(event);

        // assert
        await().atMost(Duration.ofSeconds(40)).until(() -> transport.delivered().equals(List.of(event)));
        assertThat(storage.isResolvable()).isFalse();
    }
}
