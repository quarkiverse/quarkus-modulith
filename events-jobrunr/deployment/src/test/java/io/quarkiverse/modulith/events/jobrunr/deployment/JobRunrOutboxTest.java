package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.jobrunr.jobs.states.StateName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderNoted;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order.OrderService;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.ExternalizedEvents;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.Jobs;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingBrokerTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.RecordingJobRunrTransport;
import io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support.SentMessage;
import io.quarkiverse.modulith.events.jobrunr.runtime.ExternalizationJobRequest;
import io.quarkiverse.modulith.events.jobrunr.runtime.ExternalizationJobRequestHandler;
import io.quarkus.test.QuarkusExtensionTest;

class JobRunrOutboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(40);

    @RegisterExtension
    static final QuarkusExtensionTest test = JobRunrApps.app("outbox", RecordingJobRunrTransport.class,
            RecordingBrokerTransport.class, SentMessage.class, ExternalizedEvents.class);

    @Inject
    OrderService orders;

    @Inject
    RecordingJobRunrTransport jobRunrTransport;

    @Inject
    RecordingBrokerTransport brokerTransport;

    @Inject
    ExternalizedEvents externalized;

    @Inject
    ExternalizationJobRequestHandler handler;

    @Inject
    Jobs jobs;

    @BeforeEach
    void reset() {
        await().atMost(TIMEOUT).until(() -> jobs.count(StateName.ENQUEUED) + jobs.count(StateName.PROCESSING)
                + jobs.count(StateName.SCHEDULED) == 0);
        jobRunrTransport.reset();
        brokerTransport.reset();
        externalized.reset();
    }

    @Test
    void runsOneJobPerTransportAfterTheCommit() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", List.of("book", "pen"));
        long before = jobs.count(StateName.SUCCEEDED);

        // act
        orders.place(event);

        // assert
        await().atMost(TIMEOUT).until(() -> jobRunrTransport.delivered().size() == 1
                && brokerTransport.sent().size() == 1);
        assertThat(jobRunrTransport.delivered()).containsExactly(event);
        assertThat(brokerTransport.sent()).containsExactly(new SentMessage(event, RoutingTarget.forTarget("orders")));
        await().atMost(TIMEOUT).until(() -> externalized.brokerResults().equals(List.of("ack-1")));
        await().atMost(TIMEOUT).until(() -> jobs.count(StateName.SUCCEEDED) == before + 2);
    }

    @Test
    void enqueuesNothingWhenTheTransactionRollsBack() {
        // arrange
        long before = jobs.total();

        // act
        Throwable thrown = catchThrowable(() -> orders.placeAndRollBack(new OrderPlaced("o-2", List.of("cup"))));

        // assert
        assertThat(thrown).hasMessage("rolled back on purpose");
        assertThat(jobs.total()).isEqualTo(before);
        assertThat(jobRunrTransport.attempts()).isZero();
    }

    @Test
    void enqueuesWithoutTransaction() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-3", List.of());

        // act
        orders.placeWithoutTransaction(event);

        // assert
        await().atMost(TIMEOUT).until(() -> jobRunrTransport.delivered().equals(List.of(event)));
    }

    @Test
    void ignoresEventsTheConfigurationDoesNotSupport() {
        // arrange
        long before = jobs.total();

        // act
        orders.note(new OrderNoted("o-4"));

        // assert
        assertThat(jobs.total()).isEqualTo(before);
    }

    @Test
    void retriesAFailingTransportUntilItSucceeds() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-5", List.of("lamp"));
        jobRunrTransport.failNext(2);

        // act
        orders.place(event);

        // assert
        await().atMost(TIMEOUT).until(() -> jobRunrTransport.delivered().equals(List.of(event)));
        assertThat(jobRunrTransport.attempts()).isEqualTo(3);
        assertThat(brokerTransport.sent()).containsExactly(new SentMessage(event, RoutingTarget.forTarget("orders")));
    }

    @Test
    void failsAJobOfAnUnknownTransport() {
        // arrange
        ExternalizationJobRequest request = new ExternalizationJobRequest("com.example.Gone",
                OrderPlaced.class.getName(), "{\"id\":\"o-6\",\"items\":[]}");

        // act & assert
        assertThatThrownBy(() -> handler.run(request)).isInstanceOf(IllegalStateException.class)
                .hasMessage("The job names the JobRunrExternalizationTransport com.example.Gone, which is not a bean of"
                        + " this application: add it back, or delete the job in the JobRunr dashboard");
    }

    @Test
    void failsAJobOfAnUnknownEventType() {
        // arrange
        ExternalizationJobRequest request = new ExternalizationJobRequest(RecordingJobRunrTransport.class.getName(),
                "com.example.Gone", "{}");

        // act & assert
        assertThatThrownBy(() -> handler.run(request)).isInstanceOf(ClassNotFoundException.class)
                .hasMessage("com.example.Gone");
    }
}
