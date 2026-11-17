package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventExternalized;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.externalization.TestExternalizers;
import io.quarkiverse.modulith.events.externalization.fakes.DirectExecutor;
import io.quarkiverse.modulith.events.externalization.fakes.FakeTransport;
import io.quarkiverse.modulith.events.externalization.fakes.FixedConfig;
import io.quarkiverse.modulith.events.externalization.fakes.FixedEventsConfig;
import io.quarkiverse.modulith.events.externalization.fakes.FixedOutbox;
import io.quarkiverse.modulith.events.externalization.fakes.RecordingEvent;
import io.quarkiverse.modulith.events.externalization.fakes.RejectingExecutor;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

class OutboxDrainTest {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
    private final DirectExecutor executor = new DirectExecutor();
    private static final Duration SEND_TIMEOUT = Duration.ofMillis(200);

    private OutboxDrain drain(Supplier<CompletionStage<?>> answer, ExternalizationPublications publications) {
        return drain(new FakeTransport(answer), publications, TestPublications.NOW, 100);
    }

    private OutboxDrain drain(FakeTransport transport, ExternalizationPublications publications, Instant now,
            int batchSize) {
        return drain(new OutboxDrain(), transport, publications, now, batchSize, OptionalInt.empty());
    }

    private OutboxDrain drain(OutboxDrain drain, FakeTransport transport, ExternalizationPublications publications,
            Instant now, int batchSize, OptionalInt abandonAfterAttempts) {
        drain.publications = TestPublications.initialize(publications, Optional.of(repository), now);
        drain.externalizer = TestExternalizers.of(transport);
        drain.config = new FixedConfig(false, new FixedOutbox(SEND_TIMEOUT, Duration.ofMinutes(1), batchSize));
        drain.eventsConfig = new FixedEventsConfig(abandonAfterAttempts);
        drain.executor = executor;
        return drain;
    }

    private static FakeTransport acknowledging() {
        return new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
    }

    private TargetEventPublication record(String event, int secondsAgo) {
        return repository.create(TargetEventPublication.of(event,
                PublicationTargetIdentifier.of(OutboxEventExternalizer.LISTENER_ID),
                TestPublications.NOW.minusSeconds(secondsAgo)));
    }

    private static List<Object> payloads(FakeTransport transport) {
        return transport.sent().stream().map(sent -> sent.get(0)).toList();
    }

    private TargetEventPublication record(String event) {
        return repository.create(TargetEventPublication.of(event,
                PublicationTargetIdentifier.of(OutboxEventExternalizer.LISTENER_ID), Instant.now()));
    }

    @Test
    void stopsWithoutSchedule() {
        // arrange
        OutboxDrain drain = drain(() -> CompletableFuture.completedFuture("ack"), new ExternalizationPublications());

        // act
        drain.stop();

        // assert
        assertThat(drain.schedule).isNull();
    }

    @Test
    void runsAgainWhenTriggeredDuringARun() {
        // arrange
        List<String> runs = new ArrayList<>();
        OutboxDrain counting = new CountingDrain(runs);
        counting.executor = executor;

        // act
        counting.trigger();

        // assert
        assertThat(runs).containsExactly("run 1 triggers again", "run 2");
        assertThat(executor.executed()).isEqualTo(1);
        assertThat(counting.requests.get()).isZero();
    }

    @Test
    void skipsPublicationsAnotherInstanceClaimed() {
        // arrange
        record("hello");
        OutboxDrain drain = drain(() -> CompletableFuture.completedFuture("ack"), new FailingPublications("claim"));

        // act
        drain.runOnce();

        // assert
        assertThat(repository.findByStatus(Status.PUBLISHED)).hasSize(1);
    }

    @Test
    void survivesFailingRepositories() {
        // arrange
        record("hello");
        OutboxDrain drain = drain(() -> CompletableFuture.completedFuture("ack"), new FailingPublications("find"));

        // act
        drain.runOnce();

        // assert
        assertThat(repository.findByStatus(Status.PUBLISHED)).hasSize(1);
    }

    @Test
    void marksThePublicationFailedWhenInterrupted() {
        // arrange
        TargetEventPublication recorded = record("hello");
        OutboxDrain drain = drain(CompletableFuture::new, new ExternalizationPublications());
        Thread.currentThread().interrupt();

        // act
        boolean sent = drain.send(recorded.getIdentifier(), "hello");

        // assert
        assertThat(Thread.interrupted()).isTrue();
        assertThat(sent).isFalse();
        assertThat(repository.findByStatus(Status.FAILED)).hasSize(1);
    }

    @Test
    void logsRepeatedFailuresOfAPublicationOnce() {
        // arrange
        TargetEventPublication recorded = record("hello");
        OutboxDrain drain = drain(() -> CompletableFuture.failedFuture(new IllegalStateException("down")),
                new ExternalizationPublications());

        // act
        boolean first = drain.send(recorded.getIdentifier(), "hello");
        boolean second = drain.send(recorded.getIdentifier(), "hello");

        // assert
        assertThat(first).isFalse();
        assertThat(second).isFalse();
        assertThat(drain.lastFailure).isEqualTo(recorded.getIdentifier());
        assertThat(repository.findByStatus(Status.FAILED)).singleElement()
                .satisfies(publication -> assertThat(publication.getCompletionAttempts()).isEqualTo(2));
    }

    @Test
    void pagesThroughTheBacklogOldestFirst() {
        // arrange
        record("third", 30);
        record("first", 50);
        record("fifth", 10);
        record("second", 40);
        record("fourth", 20);
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, 2);

        // act
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("first", "second", "third", "fourth", "fifth");
        assertThat(repository.findCompletedPublications()).hasSize(5);
    }

    @Test
    void sendsEachPublicationOnceWhenTwoDrainsShareOneTable() {
        // arrange
        record("first", 20);
        record("second", 10);
        FakeTransport other = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        OutboxDrain otherDrain = drain(other, new ExternalizationPublications(), TestPublications.NOW, 100);
        List<String> interleaved = new ArrayList<>();
        FakeTransport transport = new FakeTransport(() -> {
            otherDrain.runOnce();
            interleaved.add("other drain ran, sent " + other.sent().size());
            return CompletableFuture.completedFuture("ack");
        });
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, 100);

        // act
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("first", "second");
        assertThat(other.sent()).isEmpty();
        assertThat(interleaved).containsExactly("other drain ran, sent 0", "other drain ran, sent 0");
        assertThat(repository.findCompletedPublications()).hasSize(2);
    }

    @Test
    void takesOverPublicationsWhoseDrainDiedAfterClaimingThem() {
        // arrange
        TargetEventPublication abandoned = record("abandoned", 700);
        ExternalizationPublications dead = TestPublications.initialize(new ExternalizationPublications(),
                Optional.of(repository), TestPublications.NOW.minusSeconds(600));
        dead.claim(abandoned.getIdentifier(), Duration.ofMinutes(1));
        dead.markProcessing(abandoned.getIdentifier());
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, 100);

        // act
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("abandoned");
        assertThat(repository.findCompletedPublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getCompletionAttempts()).isEqualTo(2));
    }

    @Test
    void leavesFreshClaimsOfAnotherDrainAlone() {
        // arrange
        TargetEventPublication claimed = record("claimed", 20);
        record("later", 10);
        ExternalizationPublications other = TestPublications.initialize(new ExternalizationPublications(),
                Optional.of(repository), TestPublications.NOW.minusSeconds(5));
        other.claim(claimed.getIdentifier(), Duration.ofMinutes(1));
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, 100);

        // act
        drain.runOnce();

        // assert
        assertThat(transport.sent()).isEmpty();
        assertThat(repository.findByStatus(Status.RESUBMITTED)).extracting(TargetEventPublication::getIdentifier)
                .containsExactly(claimed.getIdentifier());
        assertThat(repository.findByStatus(Status.PUBLISHED)).hasSize(1);
    }

    @Test
    void failsAndSkipsPublicationsWhoseEventCannotBeRead() {
        // arrange
        TargetEventPublication poison = record("poison", 20);
        record("next", 10);
        FakeTransport transport = acknowledging();
        OutboxDrain drain = drain(transport, new UnreadablePublications(), TestPublications.NOW, 100);

        // act
        drain.runOnce();
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("next");
        assertThat(repository.findByStatus(Status.FAILED)).singleElement().satisfies(failed -> {
            assertThat(failed.getIdentifier()).isEqualTo(poison.getIdentifier());
            assertThat(failed.getCompletionAttempts()).isEqualTo(2);
        });
        assertThat(drain.lastFailure).isEqualTo(poison.getIdentifier());
    }

    @Test
    void stopsAtAnUnreadablePublicationAnotherInstanceClaimed() {
        // arrange
        TargetEventPublication poison = record("poison", 20);
        record("next", 10);
        ExternalizationPublications other = TestPublications.initialize(new ExternalizationPublications(),
                Optional.of(repository), TestPublications.NOW.minusSeconds(5));
        other.claim(poison.getIdentifier(), Duration.ofMinutes(1));
        FakeTransport transport = acknowledging();
        OutboxDrain drain = drain(transport, new UnreadablePublications(), TestPublications.NOW, 100);

        // act
        drain.runOnce();

        // assert
        assertThat(transport.sent()).isEmpty();
        assertThat(repository.findByStatus(Status.RESUBMITTED)).extracting(TargetEventPublication::getEvent)
                .containsExactly("poison");
    }

    @Test
    void skipsAbandonedPublicationsAndWarnsOnce() {
        // arrange
        TargetEventPublication rejected = record("rejected", 20);
        record("next", 10);
        repository.markProcessing(rejected.getIdentifier());
        repository.markProcessing(rejected.getIdentifier());
        repository.markFailed(rejected.getIdentifier());
        FakeTransport transport = acknowledging();
        OutboxDrain drain = drain(new OutboxDrain(), transport, new ExternalizationPublications(),
                TestPublications.NOW, 100, OptionalInt.of(2));

        // act
        drain.runOnce();
        record("later", 5);
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("next", "later");
        assertThat(drain.reportedAbandoned).containsExactly(rejected.getIdentifier());
        assertThat(repository.findByStatus(Status.FAILED)).singleElement()
                .satisfies(failed -> assertThat(failed.getCompletionAttempts()).isEqualTo(2));
    }

    @Test
    void retriesAPublicationBelowTheAttemptLimitInOrder() {
        // arrange
        TargetEventPublication rejected = record("rejected", 20);
        record("next", 10);
        repository.markProcessing(rejected.getIdentifier());
        repository.markFailed(rejected.getIdentifier());
        FakeTransport transport = new FakeTransport(
                () -> CompletableFuture.failedFuture(new IllegalStateException("message too large")));
        OutboxDrain drain = drain(new OutboxDrain(), transport, new ExternalizationPublications(),
                TestPublications.NOW, 100, OptionalInt.of(3));

        // act
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("rejected");
        assertThat(drain.reportedAbandoned).isEmpty();
        assertThat(repository.findByStatus(Status.FAILED)).singleElement()
                .satisfies(failed -> assertThat(failed.getCompletionAttempts()).isEqualTo(2));
    }

    @Test
    void dropsTheRequestWhenTheExecutorRefusesTheRun() {
        // arrange
        OutboxDrain drain = drain(acknowledging(), new ExternalizationPublications(), TestPublications.NOW, 100);
        drain.executor = new RejectingExecutor();
        record("hello");

        // act
        drain.trigger();
        int afterRefusal = drain.requests.get();
        drain.executor = executor;
        drain.trigger();

        // assert
        assertThat(afterRefusal).isZero();
        assertThat(executor.executed()).isEqualTo(1);
        assertThat(repository.findCompletedPublications()).hasSize(1);
    }

    @Test
    void resetsTheRequestsWhenARunEndsWithAnError() {
        // arrange
        OutboxDrain drain = new BrokenDrain();
        drain.requests.set(3);

        // act & assert
        assertThatThrownBy(drain::drain).isInstanceOf(LinkageError.class).hasMessage("broken class");

        // assert
        assertThat(drain.requests.get()).isZero();
        assertThat(drain.running.isLocked()).isFalse();
    }

    @Test
    void marksThePublicationFailedWhenRecordingTheCompletionFails() {
        // arrange
        TargetEventPublication recorded = record("hello");
        OutboxDrain drain = drain(acknowledging(), new FailingPublications("completed"), TestPublications.NOW, 100);

        // act
        boolean sent = drain.send(recorded.getIdentifier(), "hello");

        // assert
        assertThat(sent).isFalse();
        assertThat(drain.lastFailure).isEqualTo(recorded.getIdentifier());
        assertThat(repository.findByStatus(Status.FAILED)).hasSize(1);
    }

    @Test
    void cancelsASendThatTimesOutSoALateAcknowledgementIsNotReported() {
        // arrange
        TargetEventPublication recorded = record("hello");
        CompletableFuture<Object> late = new CompletableFuture<>();
        FakeTransport transport = new FakeTransport(() -> late);
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, 100);
        RecordingEvent<EventExternalized<Object, Object>> fired = new RecordingEvent<>(null);
        drain.externalizer = TestExternalizers.of(transport, fired);

        // act
        boolean sent = drain.send(recorded.getIdentifier(), "hello");
        late.complete("ack");

        // assert
        assertThat(sent).isFalse();
        assertThat(fired.fired()).isEmpty();
        assertThat(repository.findByStatus(Status.FAILED)).hasSize(1);
    }

    @Test
    void stopsTakingRunsAtShutdown() {
        // arrange
        record("hello");
        FakeTransport transport = acknowledging();
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, 100);

        // act
        drain.onShutdown(null);
        drain.trigger();
        drain.runOnce();

        // assert
        assertThat(drain.stopped).isTrue();
        assertThat(executor.executed()).isZero();
        assertThat(transport.sent()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 100 })
    void stopsARunAfterThePublicationItIsSendingAtShutdown(int batchSize) {
        // arrange
        record("first", 20);
        record("second", 10);
        AtomicReference<OutboxDrain> self = new AtomicReference<>();
        FakeTransport transport = new FakeTransport(() -> {
            self.get().stopped = true;
            return CompletableFuture.completedFuture("ack");
        });
        OutboxDrain drain = drain(transport, new ExternalizationPublications(), TestPublications.NOW, batchSize);
        self.set(drain);

        // act
        drain.runOnce();

        // assert
        assertThat(payloads(transport)).containsExactly("first");
        assertThat(repository.findByStatus(Status.PUBLISHED)).extracting(TargetEventPublication::getEvent)
                .containsExactly("second");
    }

    @Test
    void waitsForTheCurrentRunAtShutdownUpToTheTimeout() throws Exception {
        // arrange
        OutboxDrain drain = new OutboxDrain();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread run = Thread.ofVirtual().start(() -> {
            drain.running.lock();
            try {
                locked.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                drain.running.unlock();
            }
        });
        locked.await();

        // act
        boolean whileRunning = drain.awaitCurrentRun(Duration.ofMillis(50));
        release.countDown();
        run.join();
        boolean afterTheRun = drain.awaitCurrentRun(Duration.ofMillis(50));

        // assert
        assertThat(whileRunning).isFalse();
        assertThat(afterTheRun).isTrue();
    }

    @Test
    void stopsWaitingForTheCurrentRunWhenInterrupted() {
        // arrange
        OutboxDrain drain = new OutboxDrain();
        Thread.currentThread().interrupt();

        // act
        boolean ended = drain.awaitCurrentRun(Duration.ofSeconds(1));

        // assert
        assertThat(ended).isFalse();
        assertThat(Thread.interrupted()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "PT0S, PT1S, PT1M, 1, 'quarkus.modulith.events.externalization.outbox.interval must be positive, got PT0S'",
            "PT5S, -PT1S, PT1M, 1, 'quarkus.modulith.events.externalization.outbox.send-timeout must be positive, got PT-1S'",
            "PT5S, PT1S, -PT1M, 1, 'quarkus.modulith.events.externalization.outbox.stale-after must not be negative, got PT-1M'",
            "PT5S, PT1S, PT1M, 0, 'quarkus.modulith.events.externalization.outbox.batch-size must be at least 1, got 0'"
    })
    void rejectsOutboxSettingsEveryRunWouldFailOn(Duration interval, Duration sendTimeout, Duration staleAfter,
            int batchSize, String message) {
        // arrange
        ExternalizationRuntimeConfig.Outbox outbox = new IntervalOutbox(interval, sendTimeout, staleAfter, batchSize);

        // act & assert
        assertThatThrownBy(() -> OutboxDrain.validate(outbox)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    @Test
    void acceptsAnOutboxWithoutStaleDelay() {
        // arrange
        ExternalizationRuntimeConfig.Outbox outbox = new IntervalOutbox(Duration.ofSeconds(5), Duration.ofSeconds(1),
                Duration.ZERO, 1);

        // act
        OutboxDrain.validate(outbox);

        // assert
        assertThat(outbox.staleAfter()).isZero();
    }

    @ParameterizedTest
    @CsvSource({ "40001, 0", "08006, 0" })
    void stopsTheRunWhenAClaimFails(String sqlState, int sent) {
        // arrange
        record("hello");
        FakeTransport transport = acknowledging();
        OutboxDrain drain = drain(transport, new ConflictingPublications(sqlState), TestPublications.NOW, 100);

        // act
        drain.runOnce();

        // assert
        assertThat(transport.sent()).hasSize(sent);
        assertThat(repository.findByStatus(Status.PUBLISHED)).hasSize(1);
    }
}
