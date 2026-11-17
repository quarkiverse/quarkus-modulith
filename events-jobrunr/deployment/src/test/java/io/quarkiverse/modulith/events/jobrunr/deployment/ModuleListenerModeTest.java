package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingBrokerTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingJobRunrTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.SentMessage;
import io.quarkiverse.modulith.events.jobrunr.runtime.JobRunrEventExternalizer;
import io.quarkiverse.modulith.events.jobrunr.runtime.TransactionalStorageProviderProducer;
import io.quarkus.test.QuarkusExtensionTest;

class ModuleListenerModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("module-listener", RecordingJobRunrTransport.class,
            RecordingBrokerTransport.class, SentMessage.class);

    @Inject
    OrderService orders;

    @Inject
    RecordingBrokerTransport brokerTransport;

    @Inject
    RecordingJobRunrTransport jobRunrTransport;

    @Inject
    Instance<JobRunrEventExternalizer> jobRunrExternalizer;

    @Inject
    Instance<TransactionalStorageProviderProducer> storage;

    @Test
    void leavesExternalizationToTheModuleListener() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", List.of());

        // act
        orders.place(event);

        // assert
        await().atMost(Duration.ofSeconds(5)).until(() -> brokerTransport.sent().size() == 1);
        assertThat(brokerTransport.sent()).containsExactly(new SentMessage(event, RoutingTarget.forTarget("orders")));
        assertThat(jobRunrTransport.attempts()).isZero();
        assertThat(jobRunrExternalizer.isResolvable()).isFalse();
        assertThat(storage.isResolvable()).isFalse();
    }
}
