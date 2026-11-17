package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.externalization.TestExternalizers;
import io.quarkiverse.modulith.events.externalization.fakes.DirectExecutor;
import io.quarkiverse.modulith.events.externalization.fakes.FakeTransport;
import io.quarkiverse.modulith.events.externalization.fakes.QueuedExecutor;
import io.quarkiverse.modulith.events.externalization.fakes.RejectingExecutor;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.IOThreadDetector;

class EventExternalizerModuleListenerTest {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
    private final DirectExecutor executor = new DirectExecutor();

    @AfterEach
    void allowBlockingAgain() {
        BlockingOperationControl.setIoThreadDetector(new IOThreadDetector[0]);
    }

    private EventExternalizerModuleListener listener(FakeTransport transport,
            ExternalizationPublications publications) {
        EventExternalizerModuleListener listener = new EventExternalizerModuleListener();
        listener.configuration = TestExternalizers.STRINGS;
        listener.externalizer = TestExternalizers.of(transport);
        listener.publications = TestPublications.initialize(publications, Optional.of(repository));
        listener.transactions = new NoExternalizationTransactions();
        listener.executor = executor;
        return listener;
    }

    private static FakeTransport acknowledging() {
        return new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
    }

    @Test
    void storesOnAWorkerThreadWhenCalledOnAnIoThread() {
        // arrange
        FakeTransport transport = acknowledging();
        EventExternalizerModuleListener listener = listener(transport, new ExternalizationPublications());
        BlockingOperationControl.setIoThreadDetector(new IOThreadDetector[] { () -> true });

        // act
        listener.onEvent("hello");

        // assert
        assertThat(executor.executed()).isEqualTo(1);
        assertThat(transport.sent()).extracting(sent -> sent.get(1)).containsExactly(RoutingTarget.forTarget("strings"));
        assertThat(repository.findCompletedPublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getListenerId())
                        .isEqualTo(EventExternalizerModuleListener.LISTENER_ID));
    }

    @Test
    void ignoresEventsTheConfigurationDoesNotSelect() {
        // arrange
        FakeTransport transport = acknowledging();
        EventExternalizerModuleListener listener = listener(transport, new ExternalizationPublications());

        // act
        listener.onEvent(42);

        // assert
        assertThat(transport.sent()).isEmpty();
        assertThat(repository.findIncompletePublications()).isEmpty();
    }

    @Test
    void sendsNothingWhenThePublicationCannotBeMarked() {
        // arrange
        FakeTransport transport = acknowledging();
        EventExternalizerModuleListener listener = listener(transport, new FailingPublications("processing"));

        // act
        listener.onEvent("hello");

        // assert
        assertThat(transport.sent()).isEmpty();
        assertThat(repository.findByStatus(Status.PUBLISHED)).hasSize(1);
    }

    @Test
    void survivesFailuresToRecordTheOutcome() {
        // arrange
        FakeTransport transport = acknowledging();
        EventExternalizerModuleListener listener = listener(transport, new FailingPublications("completed"));

        // act
        listener.onEvent("hello");

        // assert
        assertThat(transport.sent()).hasSize(1);
        assertThat(repository.findByStatus(Status.PROCESSING)).hasSize(1);
    }

    @Test
    void sendsWithoutPublicationAndOnlyLogsFailures() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.failedFuture(new IllegalStateException()));
        EventExternalizerModuleListener listener = new EventExternalizerModuleListener();
        listener.configuration = TestExternalizers.STRINGS;
        listener.externalizer = TestExternalizers.of(transport);
        listener.publications = TestPublications.initialize(new ExternalizationPublications(), Optional.empty());
        listener.transactions = new NoExternalizationTransactions();
        listener.executor = executor;

        // act
        listener.onEvent("hello");

        // assert
        assertThat(transport.sent()).hasSize(1);
        assertThat(repository.findIncompletePublications()).isEmpty();
    }

    @Test
    void resubmitsStoredPublications() {
        // arrange
        FakeTransport transport = acknowledging();
        EventExternalizerModuleListener listener = listener(transport, new ExternalizationPublications());
        UUID stored = listener.publications.store("hello", EventExternalizerModuleListener.LISTENER_ID)
                .orElseThrow();

        // act
        listener.resubmit(stored, "hello");

        // assert
        assertThat(repository.findCompletedPublications()).extracting(TargetEventPublication::getIdentifier)
                .containsExactly(stored);
        assertThat(listener.handles(EventExternalizerModuleListener.SPRING_LISTENER_ID)).isTrue();
        assertThat(listener.handles(OutboxEventExternalizer.LISTENER_ID)).isFalse();
        assertThat(List.of(EventExternalizerModuleListener.LISTENER_ID)).containsExactly(
                "io.quarkiverse.modulith.events.externalization.runtime.EventExternalizerModuleListener"
                        + ".externalize(java.lang.Object)");
    }

    private EventExternalizerModuleListener serialized(FakeTransport transport, ExternalizationPublications publications,
            ListenerExecutor workers) {
        EventExternalizerModuleListener listener = listener(transport, publications);
        listener.externalizer = TestExternalizers.serialized(transport);
        listener.executor = workers;
        return listener;
    }

    private static List<Object> payloads(FakeTransport transport) {
        return transport.sent().stream().map(sent -> sent.get(0)).toList();
    }

    @Test
    void sendsSerializedEventsInPublicationOrderWhateverOrderTheWorkersRunIn() {
        // arrange
        FakeTransport transport = acknowledging();
        QueuedExecutor workers = new QueuedExecutor();
        EventExternalizerModuleListener listener = serialized(transport, new ExternalizationPublications(), workers);
        List<String> published = List.of("e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7");
        published.forEach(listener::onEvent);

        // act
        for (int position : List.of(5, 2, 7, 0, 3, 6, 1, 4)) {
            workers.run(position);
        }

        // assert
        assertThat(payloads(transport)).containsExactlyElementsOf(published);
        assertThat(repository.findCompletedPublications()).hasSize(published.size());
    }

    @Test
    void givesUpTheTurnWhenTheWorkersRejectTheSend() {
        // arrange
        FakeTransport transport = acknowledging();
        EventExternalizerModuleListener listener = serialized(transport, new ExternalizationPublications(),
                new RejectingExecutor());
        Throwable rejected = catchThrowable(() -> listener.onEvent("rejected"));
        listener.executor = executor;

        // act
        listener.onEvent("next");

        // assert
        assertThat(rejected).isInstanceOf(RejectedExecutionException.class);
        assertThat(payloads(transport)).containsExactly("next");
    }

    @Test
    void givesUpTheTurnWhenStoringOnTheWorkerFails() {
        // arrange
        FakeTransport transport = acknowledging();
        QueuedExecutor workers = new QueuedExecutor();
        EventExternalizerModuleListener listener = serialized(transport, new FailingPublications("store"), workers);
        BlockingOperationControl.setIoThreadDetector(new IOThreadDetector[] { () -> true });
        listener.onEvent("lost");
        Throwable lost = catchThrowable(() -> workers.run(0));
        listener.publications = TestPublications.initialize(new ExternalizationPublications(), Optional.of(repository));

        // act
        listener.onEvent("next");
        workers.run(1);

        // assert
        assertThat(lost).isInstanceOf(IllegalStateException.class).hasMessage("store failed");
        assertThat(payloads(transport)).containsExactly("next");
    }

    @Test
    void givesUpTheTurnWhenThePublicationCannotBeMarked() {
        // arrange
        FakeTransport transport = acknowledging();
        QueuedExecutor workers = new QueuedExecutor();
        EventExternalizerModuleListener listener = serialized(transport, new FailingPublications("processing"),
                workers);
        listener.onEvent("unmarked");
        workers.run(0);
        listener.publications = TestPublications.initialize(new ExternalizationPublications(), Optional.of(repository));

        // act
        listener.onEvent("next");
        workers.run(1);

        // assert
        assertThat(payloads(transport)).containsExactly("next");
        assertThat(repository.findCompletedPublications()).hasSize(1);
    }

    @Test
    void logsAnOutageOnceAndNoticesTheRecovery() {
        // arrange
        List<Boolean> acknowledged = List.of(false, false, true, false);
        AtomicInteger calls = new AtomicInteger();
        FakeTransport transport = new FakeTransport(() -> acknowledged.get(calls.getAndIncrement())
                ? CompletableFuture.completedFuture("ack")
                : CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        EventExternalizerModuleListener listener = listener(transport, new ExternalizationPublications());
        List<Boolean> failing = new ArrayList<>();

        // act
        for (String event : List.of("first", "second", "third", "fourth")) {
            listener.onEvent(event);
            failing.add(listener.failing.get());
        }

        // assert
        assertThat(failing).containsExactly(true, true, false, true);
        assertThat(repository.findByStatus(Status.FAILED)).hasSize(3);
        assertThat(repository.findCompletedPublications()).hasSize(1);
    }
}
