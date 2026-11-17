package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.invoke.Invoker;
import jakarta.transaction.Transactional.TxType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.spi.EventPublicationRegistry;
import io.quarkiverse.modulith.runtime.cdi.FakeBean;
import io.quarkiverse.modulith.runtime.cdi.FakeHandle;
import io.quarkiverse.modulith.runtime.cdi.FakeInstance;

class ModuleEventDispatcherTest {

    final List<Object> calls = new CopyOnWriteArrayList<>();
    final List<String> contextCalls = new ArrayList<>();
    final FakeTransactions transactions = new FakeTransactions();
    final FakeRegistry registry = new FakeRegistry();
    final FakeExecutor executor = new FakeExecutor();

    @Test
    void dispatchesAnObservedTypeToItsListenersOnly() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                listener("by-interface", Shipped.class, false), listener("by-class", OrderShipped.class, false));

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        dispatcher.dispatch(new FakeEventContext(new OrderShipped("o-2"), Set.of()), "com.acme.NotListened");

        // assert
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(contextCalls).containsExactly("activate", "terminate");
    }

    @Test
    void listsTheListenerIdsInTableOrder() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                listener("by-interface", Shipped.class, false), listener("by-class", OrderShipped.class, false));

        // act
        Set<String> ids = dispatcher.listenerIds();

        // assert
        assertThat(ids).containsExactly("by-interface", "by-class");
    }

    @Test
    void initResolvesTheCollaboratorsAndBecomesTheCurrentDispatcher() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER);
        dispatcher.registries = instance(EventPublicationRegistry.class, registry);
        dispatcher.readOnlyTransactions = new FakeInstance<>(List.of());

        // act
        dispatcher.init();
        Optional<ModuleEventDispatcher> current = ModuleEventObservers.dispatcher();
        dispatcher.destroy();
        Optional<ModuleEventDispatcher> afterDestroy = ModuleEventObservers.dispatcher();

        // assert
        assertThat(dispatcher.registry).containsSame(registry);
        assertThat(dispatcher.readOnly).isEmpty();
        assertThat(current).containsSame(dispatcher);
        assertThat(afterDestroy).isEmpty();
    }

    @Test
    void refusesMoreThanOneEventPublicationRegistry() {
        // arrange
        Instance<EventPublicationRegistry> registries = instance(EventPublicationRegistry.class, registry,
                new FakeRegistry());

        // act & assert
        assertThatThrownBy(() -> ModuleEventDispatcher.optional(registries, EventPublicationRegistry.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("More than one bean implements " + EventPublicationRegistry.class.getName()
                        + ", the module event dispatcher needs one at most");
    }

    @Test
    void dropsEventsFiredWhenNoContainerRuns() {
        // arrange
        FakeEventContext context = new FakeEventContext(new OrderShipped("o-1"), Set.of());

        // act
        ModuleEventObservers.notify(context, OrderShipped.class.getName());

        // assert
        assertThat(ModuleEventObservers.dispatcher()).isEmpty();
        assertThat(calls).isEmpty();
    }

    @Test
    void storesInTheTransactionAndDeliversAfterTheCommit() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        transactions.active = true;
        transactions.activeInListener = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        List<Object> beforeCommit = List.copyOf(calls);
        transactions.commit();

        // assert
        assertThat(beforeCommit).isEmpty();
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(registry.calls).containsExactly("store l", "processing 1", "completed 1");
        assertThat(transactions.calls).containsExactly("new", "REQUIRES_NEW");
    }

    @Test
    void deliversAfterTheCommitWithoutRegistry() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        transactions.active = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        List<Object> beforeCommit = List.copyOf(calls);
        transactions.commit();

        // assert
        assertThat(beforeCommit).isEmpty();
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(transactions.calls).containsExactly("REQUIRES_NEW");
    }

    @ParameterizedTest
    @CsvSource({
            "IN_LISTENER, true, 'new,REQUIRES_NEW', 'store l,processing 1,completed 1'",
            "IN_LISTENER, false, 'new,REQUIRES_NEW,new', 'store l,processing 1,completed 1'",
            "IN_INTERCEPTOR, true, 'new,REQUIRES_NEW,new', 'store l,processing 1,completed 1'"
    })
    void completesInTheListenerTransactionOrInANewOne(CompletionTiming timing, boolean activeInListener,
            String transactionCalls, String registryCalls) {
        // arrange
        TestDispatcher dispatcher = dispatcher(timing, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        transactions.activeInListener = activeInListener;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));

        // assert
        assertThat(transactions.calls).containsExactly(transactionCalls.split(","));
        assertThat(registry.calls).containsExactly(registryCalls.split(","));
    }

    @Test
    void leavesThePublicationIncompleteWhenTheCompletionAfterASuccessfulListenerFails() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_INTERCEPTOR,
                listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        registry.failMarkCompleted = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(registry.calls).containsExactly("store l", "processing 1", "completed 1");
        assertThat(contextCalls).containsExactly("activate", "terminate");
        assertThat(idle).isTrue();
    }

    @Test
    void skipsTheListenerWhenThePublicationCannotBeClaimed() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        registry.claimable = false;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(calls).isEmpty();
        assertThat(registry.calls).containsExactly("store l", "processing 1");
        assertThat(idle).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "40001, 'store l,processing 1'", "23505, 'store l,processing 1,failed 1 claim failed'" })
    void treatsASerializationFailureOnTheClaimAsLost(String sqlState, String expected) {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        registry.claimFailure = new IllegalStateException("claim failed", new SQLException("conflict", sqlState));

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));

        // assert
        assertThat(calls).isEmpty();
        assertThat(registry.calls).containsExactly(expected.split(","));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void marksThePublicationFailedWhenTheListenerThrows(Exception failure, boolean storeDown) {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                new Listener(data("l", OrderShipped.class, false, TxType.REQUIRES_NEW, false),
                        new RecordingInvoker(calls, failure), Optional.empty()));
        dispatcher.registry = Optional.of(registry);
        registry.failMarkFailed = storeDown;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(registry.calls).containsExactly("store l", "processing 1", "failed 1 " + failure.getMessage());
        assertThat(contextCalls).containsExactly("activate", "terminate");
        assertThat(idle).isTrue();
    }

    static List<Object[]> failures() {
        return List.of(new Object[] { new IllegalStateException("runtime failure"), false },
                new Object[] { new Exception("checked failure"), true });
    }

    @Test
    void letsAnErrorPropagateAfterReleasingTheDelivery() {
        // arrange
        LinkageError error = new LinkageError("broken class");
        Invoker<Object, Object> broken = (instance, arguments) -> {
            throw error;
        };
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                new Listener(data("l", OrderShipped.class, false, TxType.REQUIRES_NEW, false), broken,
                        Optional.empty()));
        dispatcher.registry = Optional.of(registry);

        // act & assert
        assertThatThrownBy(() -> dispatch(dispatcher, new OrderShipped("o-1"))).isSameAs(error);
        assertThat(registry.calls).containsExactly("store l", "processing 1");
        assertThat(contextCalls).containsExactly("activate", "terminate");
        assertThat(dispatcher.awaitIdle(Duration.ZERO)).isTrue();
    }

    @Test
    void listenerFailuresWithoutRegistryAreOnlyLogged() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                new Listener(data("l", OrderShipped.class, false, TxType.REQUIRES_NEW, false),
                        new RecordingInvoker(calls, new IllegalStateException("boom")), Optional.empty()));
        dispatcher.contextActive = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));

        // assert
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(contextCalls).containsExactly("already-active");
        assertThat(registry.calls).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void makesReadOnlyListenerTransactionsReadOnlyWhenAStoreSupportsIt(boolean storeSupportsReadOnly) {
        // arrange
        List<String> readOnlyCalls = new ArrayList<>();
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                new Listener(data("l", OrderShipped.class, false, TxType.SUPPORTS, true),
                        new RecordingInvoker(calls, null), Optional.empty()));
        dispatcher.readOnly = storeSupportsReadOnly ? Optional.of(new FakeReadOnly(readOnlyCalls)) : Optional.empty();

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));

        // assert
        assertThat(transactions.calls).containsExactly("SUPPORTS");
        assertThat(readOnlyCalls).isEqualTo(storeSupportsReadOnly ? List.of("read-only") : List.of());
    }

    @Test
    void appliesConditionsBeforeStoring() {
        // arrange
        Predicate<Object> onlyO2 = event -> ((OrderShipped) event).id().equals("o-2");
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                new Listener(data("conditional", OrderShipped.class, false, TxType.REQUIRES_NEW, false),
                        new RecordingInvoker(calls, null), Optional.of(onlyO2)),
                listener("always", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        dispatch(dispatcher, new OrderShipped("o-2"));

        // assert
        assertThat(registry.calls).filteredOn(call -> call.startsWith("store")).containsExactly("store always",
                "store conditional", "store always");
    }

    @Test
    void storesNothingWhenNoConditionHolds() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER,
                new Listener(data("never", OrderShipped.class, false, TxType.REQUIRES_NEW, false),
                        new RecordingInvoker(calls, null), Optional.of(event -> false)));
        dispatcher.registry = Optional.of(registry);

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));

        // assert
        assertThat(calls).isEmpty();
        assertThat(registry.calls).isEmpty();
    }

    @Test
    void runsAsynchronousListenersOnTheExecutor() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, true));

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        boolean idleBefore = dispatcher.awaitIdle(Duration.ofMillis(1));
        executor.runAll();
        boolean idleAfter = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(idleBefore).isFalse();
        assertThat(calls).containsExactly(new OrderShipped("o-1"));
        assertThat(idleAfter).isTrue();
    }

    @Test
    void leavesThePublicationIncompleteWhenTheExecutorRejectsAnAsynchronousDelivery() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, true));
        dispatcher.registry = Optional.of(registry);
        executor.reject = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(calls).isEmpty();
        assertThat(registry.calls).containsExactly("store l");
        assertThat(idle).isTrue();
    }

    @Test
    void movesWorkFiredOnAnIoThreadToTheExecutor() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        dispatcher.blockingAllowed = false;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        List<String> beforeExecutor = List.copyOf(registry.calls);
        executor.runAll();
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(beforeExecutor).isEmpty();
        assertThat(registry.calls).containsExactly("store l", "processing 1", "completed 1");
        assertThat(idle).isTrue();
    }

    @Test
    void keepsWorkOnTheIoThreadWhenATransactionIsActive() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.blockingAllowed = false;
        transactions.active = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));

        // assert
        assertThat(executor.queued).isEmpty();
        assertThat(transactions.afterCommit).hasSize(1);
    }

    @Test
    void losesTheEventWhenTheExecutorRejectsWorkFromAnIoThread() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);
        dispatcher.blockingAllowed = false;
        executor.reject = true;

        // act
        dispatch(dispatcher, new OrderShipped("o-1"));
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(calls).isEmpty();
        assertThat(registry.calls).isEmpty();
        assertThat(idle).isTrue();
    }

    @Test
    void redeliversToAKnownListenerOnly() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));
        dispatcher.registry = Optional.of(registry);

        // act
        boolean known = dispatcher.redeliver(new UUID(0, 7), "l", new OrderShipped("o-1"));
        boolean unknown = dispatcher.redeliver(new UUID(0, 8), "gone", new OrderShipped("o-2"));

        // assert
        assertThat(known).isTrue();
        assertThat(unknown).isFalse();
        assertThat(registry.calls).containsExactly("processing 7", "completed 7");
    }

    @Test
    void logsARedeliveryWithoutRegistryInsteadOfCallingTheListener() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, false));

        // act
        boolean accepted = dispatcher.redeliver(new UUID(0, 7), "l", new OrderShipped("o-1"));
        boolean idle = dispatcher.awaitIdle(Duration.ZERO);

        // assert
        assertThat(accepted).isTrue();
        assertThat(calls).isEmpty();
        assertThat(contextCalls).containsExactly("activate", "terminate");
        assertThat(idle).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "true, 0", "false, 1" })
    void stopsTakingDeliveriesAtShutdownAndWaitsForTheRunningOnes(boolean idle, int running) {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, true));
        if (!idle) {
            dispatch(dispatcher, new OrderShipped("running"));
        }

        // act
        dispatcher.onShutdown(null);
        dispatch(dispatcher, new OrderShipped("late"));
        dispatch(dispatcher, new OrderShipped("later"));

        // assert
        assertThat(executor.queued).hasSize(running);
        assertThat(dispatcher.abandoned()).isEqualTo(2);
        assertThat(calls).isEmpty();
    }

    @Test
    void countsADeliverySubmittedWhileTheShutdownWaits() {
        // arrange
        TestDispatcher dispatcher = dispatcher(CompletionTiming.IN_LISTENER, listener("l", OrderShipped.class, true));
        dispatcher.config = new DuringShutdownConfig(() -> dispatch(dispatcher, new OrderShipped("during")));

        // act
        dispatcher.onShutdown(null);

        // assert
        assertThat(dispatcher.abandoned()).isEqualTo(1);
        assertThat(executor.queued).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("qualifiers")
    void recognizesContextLifecycleEvents(Set<Annotation> qualifiers, boolean lifecycle) {
        // arrange
        FakeEventContext context = new FakeEventContext("payload", qualifiers);

        // act
        boolean result = ModuleEventDispatcher.isLifecycleEvent(context);

        // assert
        assertThat(result).isEqualTo(lifecycle);
    }

    static List<Object[]> qualifiers() {
        return List.of(new Object[] { Set.of(Initialized.Literal.of(RequestScoped.class)), true },
                new Object[] { Set.of(BeforeDestroyed.Literal.of(RequestScoped.class)), true },
                new Object[] { Set.of(Destroyed.Literal.of(RequestScoped.class)), true },
                new Object[] { Set.of(Default.Literal.INSTANCE), false },
                new Object[] { Set.of(), false });
    }

    static void dispatch(ModuleEventDispatcher dispatcher, Object event) {
        dispatcher.dispatch(new FakeEventContext(event, Set.of()), event.getClass().getName());
    }

    TestDispatcher dispatcher(CompletionTiming timing, Listener... listeners) {
        TestDispatcher dispatcher = new TestDispatcher(contextCalls);
        List<ListenerData> data = new ArrayList<>();
        Map<String, Invoker<Object, Object>> invokers = new LinkedHashMap<>();
        Map<String, Predicate<Object>> conditions = new LinkedHashMap<>();
        for (Listener listener : listeners) {
            data.add(listener.data());
            invokers.put(listener.data().id(), listener.invoker());
            listener.condition().ifPresent(condition -> conditions.put(listener.data().id(), condition));
        }
        dispatcher.table = new ListenerTable(data);
        dispatcher.invokers = new ListenerInvokerMaps(Map.copyOf(invokers), Map.copyOf(conditions));
        dispatcher.transactions = transactions;
        dispatcher.executor = executor;
        dispatcher.config = new FakeListenerConfig(Duration.ofMillis(10), timing);
        dispatcher.bind();
        return dispatcher;
    }

    Listener listener(String id, Class<?> eventType, boolean async) {
        return new Listener(data(id, eventType, async, TxType.REQUIRES_NEW, false), new RecordingInvoker(calls, null),
                Optional.empty());
    }

    static ListenerData data(String id, Class<?> eventType, boolean async, TxType propagation, boolean readOnly) {
        return new ListenerData(id, "Listener", "on", eventType.getName(), "module", async, "Invoker",
                eventType.getName(), "", propagation, readOnly);
    }

    @SafeVarargs
    static <T> Instance<T> instance(Class<T> type, T... beans) {
        List<Instance.Handle<T>> handles = new ArrayList<>();
        for (T bean : beans) {
            handles.add(new FakeHandle<>(new FakeBean<>(type, Dependent.class), bean, new ArrayList<>()));
        }
        return new FakeInstance<>(handles);
    }
}
