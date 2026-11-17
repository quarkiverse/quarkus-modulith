package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingJobRunrTransport;
import io.quarkiverse.modulith.events.jobrunr.runtime.JobRunrEventExternalizer;
import io.quarkus.test.QuarkusExtensionTest;

class DisabledExternalizationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", RecordingJobRunrTransport.class)
            .overrideConfigKey("quarkus.modulith.events.externalization.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    RecordingJobRunrTransport transport;

    @Inject
    Instance<JobRunrEventExternalizer> externalizer;

    @Test
    void registersNothing() {
        // act
        orders.place(new OrderPlaced("o-1", List.of()));

        // assert
        assertThat(externalizer.isResolvable()).isFalse();
        assertThat(transport.attempts()).isZero();
    }
}
