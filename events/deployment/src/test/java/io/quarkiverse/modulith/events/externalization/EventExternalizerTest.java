package io.quarkiverse.modulith.events.externalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.EventExternalized;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.fakes.DirectExecutor;
import io.quarkiverse.modulith.events.externalization.fakes.FakeTransport;
import io.quarkiverse.modulith.events.externalization.fakes.FixedConfig;
import io.quarkiverse.modulith.events.externalization.fakes.RecordingEvent;
import io.quarkiverse.modulith.events.externalization.runtime.RoutingResolver;

class EventExternalizerTest {

    private static final RoutingTarget TARGET = RoutingTarget.forTarget("greetings").andKey("k");
    private static final Duration SERIALIZED_SEND_TIMEOUT = Duration.ofMillis(200);

    private final RecordingEvent<EventExternalized<Object, Object>> events = new RecordingEvent<>(null);
    private final DirectExecutor executor = new DirectExecutor();

    private static EventExternalizationConfiguration configuration(boolean serialize) {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(String.class)
                .mapping(String.class, String::toUpperCase)
                .headers(String.class, mapped -> Map.of("length", mapped.length()))
                .routeAll(event -> TARGET)
                .serializeExternalization(serialize)
                .build();
    }

    private EventExternalizer externalizer(FakeTransport transport, boolean serialize,
            RecordingEvent<EventExternalized<Object, Object>> fired) {
        EventExternalizer externalizer = new EventExternalizer();
        externalizer.configuration = configuration(serialize);
        externalizer.transport = transport;
        externalizer.routing = new RoutingResolver();
        externalizer.events = fired;
        externalizer.executor = executor;
        externalizer.runtimeConfig = new FixedConfig(serialize, SERIALIZED_SEND_TIMEOUT);
        return externalizer;
    }

    private static List<Object> payloads(FakeTransport transport) {
        return transport.sent().stream().map(sent -> sent.get(0)).toList();
    }

    @Test
    void sendsTheMappedPayloadAndFiresTheBrokerResult() throws Exception {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("offset-1"));

        // act
        EventExternalized<Object, Object> sent = externalizer(transport, false, events).externalize("hello").get();

        // assert
        assertThat(transport.sent()).containsExactly(List.of("HELLO", TARGET, Map.of("length", 5)));
        assertThat(sent.getEvent()).isEqualTo("hello");
        assertThat(sent.getMapped()).isEqualTo("HELLO");
        assertThat(sent.getTarget()).isEqualTo(TARGET);
        assertThat(sent.getBrokerResult()).isEqualTo("offset-1");
        assertThat(events.fired()).containsExactly(sent);
    }

    @Test
    void ignoresEventsTheConfigurationDoesNotSelect() throws Exception {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("unused"));

        // act
        EventExternalized<Object, Object> sent = externalizer(transport, false, events).externalize(42).get();

        // assert
        assertThat(sent).isNull();
        assertThat(transport.sent()).isEmpty();
    }

    @Test
    void failsWithoutEvent() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("unused"));

        // act
        CompletableFuture<EventExternalized<Object, Object>> sent = externalizer(transport, false, events)
                .externalize(null);

        // assert
        assertThat(sent).failsWithin(Duration.ZERO).withThrowableThat()
                .havingCause().isInstanceOf(NullPointerException.class).withMessage("event");
    }

    @Test
    void failsWhenTheTransportThrows() {
        // arrange
        IllegalStateException refused = new IllegalStateException("refused");
        FakeTransport transport = new FakeTransport(() -> {
            throw refused;
        });

        // act
        CompletableFuture<EventExternalized<Object, Object>> sent = externalizer(transport, false, events)
                .externalize("hello");

        // assert
        assertThatThrownBy(sent::get).isInstanceOf(ExecutionException.class).hasCause(refused);
        assertThat(events.fired()).isEmpty();
    }

    @Test
    void failsWhenTheTransportReturnsNothing() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> null);

        // act
        CompletableFuture<EventExternalized<Object, Object>> sent = externalizer(transport, false, events)
                .externalize("hello");

        // assert
        assertThatThrownBy(sent::get).isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The externalization transport " + FakeTransport.class.getName() + " returned null");
    }

    @Test
    void failsWhenTheBrokerRejects() {
        // arrange
        IllegalStateException nacked = new IllegalStateException("nacked");
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.failedFuture(nacked));

        // act
        CompletableFuture<EventExternalized<Object, Object>> sent = externalizer(transport, false, events)
                .externalize("hello");

        // assert
        assertThatThrownBy(sent::get).isInstanceOf(ExecutionException.class).hasCause(nacked);
        assertThat(events.fired()).isEmpty();
        assertThat(executor.executed()).isEqualTo(1);
    }

    @Test
    void completesEvenWhenAnObserverFails() throws Exception {
        // arrange
        RecordingEvent<EventExternalized<Object, Object>> failing = new RecordingEvent<>(
                new IllegalStateException("observer"));
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("offset-2"));

        // act
        EventExternalized<Object, Object> sent = externalizer(transport, false, failing).externalize("hello").get();

        // assert
        assertThat(sent.getBrokerResult()).isEqualTo("offset-2");
        assertThat(failing.fired()).containsExactly(sent);
    }

    @Test
    void serializesSendsWithoutBlockingTheCaller() throws Exception {
        // arrange
        List<CompletableFuture<Object>> acks = new ArrayList<>();
        FakeTransport transport = new FakeTransport(() -> {
            CompletableFuture<Object> ack = new CompletableFuture<>();
            acks.add(ack);
            return ack;
        });
        EventExternalizer externalizer = externalizer(transport, true, events);

        // act
        CompletableFuture<EventExternalized<Object, Object>> first = externalizer.externalize("one");
        CompletableFuture<EventExternalized<Object, Object>> second = externalizer.externalize("two");
        List<Object> sentBeforeTheFirstAck = payloads(transport);
        acks.get(0).complete("offset-1");
        List<Object> sentAfterTheFirstAck = payloads(transport);
        acks.get(1).complete("offset-2");

        // assert
        assertThat(sentBeforeTheFirstAck).containsExactly("ONE");
        assertThat(sentAfterTheFirstAck).containsExactly("ONE", "TWO");
        assertThat(first.get().getBrokerResult()).isEqualTo("offset-1");
        assertThat(second.get().getBrokerResult()).isEqualTo("offset-2");
    }

    @Test
    void sendsTheNextEventAfterAFailedOne() throws Exception {
        // arrange
        IllegalStateException nacked = new IllegalStateException("nacked");
        List<CompletableFuture<Object>> answers = List.of(CompletableFuture.failedFuture(nacked),
                CompletableFuture.completedFuture("offset-2"));
        AtomicInteger calls = new AtomicInteger();
        FakeTransport transport = new FakeTransport(() -> answers.get(calls.getAndIncrement()));
        EventExternalizer externalizer = externalizer(transport, true, events);

        // act
        CompletableFuture<EventExternalized<Object, Object>> first = externalizer.externalize("one");
        CompletableFuture<EventExternalized<Object, Object>> second = externalizer.externalize("two");

        // assert
        assertThatThrownBy(first::get).isInstanceOf(ExecutionException.class).hasCause(nacked);
        assertThat(second.get().getMapped()).isEqualTo("TWO");
        assertThat(payloads(transport)).containsExactly("ONE", "TWO");
    }

    @Test
    void failsASerializedSendTheBrokerNeverAcknowledgesAndSendsTheNext() throws Exception {
        // arrange
        CompletableFuture<Object> never = new CompletableFuture<>();
        List<CompletableFuture<Object>> answers = List.of(never, CompletableFuture.completedFuture("offset-2"));
        AtomicInteger calls = new AtomicInteger();
        FakeTransport transport = new FakeTransport(() -> answers.get(calls.getAndIncrement()));
        EventExternalizer externalizer = externalizer(transport, true, events);

        // act
        CompletableFuture<EventExternalized<Object, Object>> first = externalizer.externalize("one");
        CompletableFuture<EventExternalized<Object, Object>> second = externalizer.externalize("two");
        EventExternalized<Object, Object> next = second.get(10, TimeUnit.SECONDS);
        never.complete("late");

        // assert
        assertThatThrownBy(first::get).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TimeoutException.class);
        assertThat(next.getBrokerResult()).isEqualTo("offset-2");
        assertThat(events.fired()).containsExactly(next);
    }

    @Test
    void sendsSerializedEventsInTheOrderTheirTurnsWereTaken() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        EventExternalizer externalizer = externalizer(transport, true, events);
        EventExternalizer.Turn first = externalizer.reserve();
        EventExternalizer.Turn second = externalizer.reserve();

        // act
        CompletableFuture<EventExternalized<Object, Object>> late = externalizer.externalize("two", second);
        List<Object> sentBeforeTheFirstTurn = payloads(transport);
        externalizer.externalize("one", first);

        // assert
        assertThat(sentBeforeTheFirstTurn).isEmpty();
        assertThat(payloads(transport)).containsExactly("ONE", "TWO");
        assertThat(late).isCompleted();
    }

    @Test
    void givesUpTheTurnsOfEventsItDoesNotSend() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        EventExternalizer externalizer = externalizer(transport, true, events);
        EventExternalizer.Turn unselected = externalizer.reserve();
        EventExternalizer.Turn missing = externalizer.reserve();
        EventExternalizer.Turn sent = externalizer.reserve();
        externalizer.externalize("three", sent);
        List<Object> sentWhileTheTurnsAreHeld = payloads(transport);

        // act
        CompletableFuture<EventExternalized<Object, Object>> withoutEvent = externalizer.externalize(null, missing);
        CompletableFuture<EventExternalized<Object, Object>> notSelected = externalizer.externalize(42, unselected);

        // assert
        assertThat(sentWhileTheTurnsAreHeld).isEmpty();
        assertThat(payloads(transport)).containsExactly("THREE");
        assertThat(withoutEvent).isCompletedExceptionally();
        assertThat(notSelected).isCompletedWithValue(null);
    }

    @Test
    void takesNoTurnWithoutSerialization() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("ack"));
        EventExternalizer externalizer = externalizer(transport, false, events);
        EventExternalizer.Turn turn = externalizer.reserve();

        // act
        turn.skip();
        externalizer.externalize("hello", turn);

        // assert
        assertThat(turn.isOrdered()).isFalse();
        assertThat(payloads(transport)).containsExactly("HELLO");
    }

    @Test
    void waitsForTheAcknowledgementWhenBlocking() {
        // arrange
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.completedFuture("offset-4"));

        // act
        externalizer(transport, false, events).externalizeBlocking("hello");

        // assert
        assertThat(events.fired()).singleElement()
                .satisfies(sent -> assertThat(sent.getBrokerResult()).isEqualTo("offset-4"));
    }

    @Test
    void reportsFailuresWhenBlocking() {
        // arrange
        IllegalStateException nacked = new IllegalStateException("nacked");
        FakeTransport transport = new FakeTransport(() -> CompletableFuture.failedFuture(nacked));
        EventExternalizer externalizer = externalizer(transport, false, events);

        // act & assert
        assertThatThrownBy(() -> externalizer.externalizeBlocking("hello")).isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not externalize an event of type java.lang.String").hasCause(nacked);
    }

    @Test
    void stopsWaitingWhenInterruptedWhileBlocking() {
        // arrange
        FakeTransport transport = new FakeTransport(CompletableFuture::new);
        EventExternalizer externalizer = externalizer(transport, false, events);
        Thread.currentThread().interrupt();

        // act & assert
        assertThatThrownBy(() -> externalizer.externalizeBlocking("hello")).isInstanceOf(IllegalStateException.class)
                .hasMessage("Interrupted while externalizing an event of type java.lang.String")
                .hasCauseInstanceOf(InterruptedException.class);

        // assert
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void strategiesStoreNoPublicationsByDefault() {
        // arrange
        ExternalizationStrategy strategy = event -> {
        };

        // act
        boolean handles = strategy.handles(EventExternalizer.class.getName());

        // assert
        assertThat(handles).isFalse();

        // act & assert
        assertThatThrownBy(() -> strategy.resubmit(null, "event")).isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(strategy.getClass().getName() + " stores no event publications");
    }
}
