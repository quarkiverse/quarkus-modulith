package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Default;
import jakarta.transaction.Transactional.TxType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ModuleEventObserversTest {

    private final List<Object> calls = new CopyOnWriteArrayList<>();

    @AfterEach
    void clearTheCurrentDispatcher() {
        ModuleEventObservers.CURRENT.set(null);
    }

    @ParameterizedTest
    @MethodSource("events")
    void handsEventsToTheCurrentDispatcherExceptContextLifecycleEvents(Set<Annotation> qualifiers,
            List<Object> expected) {
        // arrange
        TestDispatcher dispatcher = new TestDispatcher(new ArrayList<>());
        dispatcher.table = new ListenerTable(List.of(new ListenerData("l", "Listener", "on", String.class.getName(),
                "module", false, "Invoker", String.class.getName(), "", TxType.SUPPORTS, false)));
        dispatcher.invokers = new ListenerInvokerMaps(Map.of("l", new RecordingInvoker(calls, null)),
                Map.of());
        dispatcher.transactions = new FakeTransactions();
        dispatcher.config = new FakeListenerConfig(Duration.ZERO, CompletionTiming.IN_LISTENER);
        dispatcher.bind();
        ModuleEventObservers.CURRENT.set(dispatcher);

        // act
        ModuleEventObservers.notify(new FakeEventContext("payload", qualifiers), String.class.getName());

        // assert
        assertThat(calls).isEqualTo(expected);
        assertThat(ModuleEventObservers.dispatcher()).isEqualTo(Optional.of(dispatcher));
    }

    static List<Object[]> events() {
        return List.of(new Object[] { Set.of(Default.Literal.INSTANCE), List.of("payload") },
                new Object[] { Set.of(Initialized.Literal.of(RequestScoped.class)), List.of() });
    }
}
