package io.quarkiverse.modulith.test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The state a {@link Scenario} waited for. Spring Modulith declares it as {@code Scenario.When.StateChangeResult}.
 *
 * @param <S> the state type
 * @param <T> the result of the stimulus
 */
public final class StateChangeResult<S, T> {

    private final When<T> when;
    private final ExecutionResult<S, T> result;

    StateChangeResult(When<T> when, ExecutionResult<S, T> result) {
        this.when = when;
        this.result = result;
    }

    /**
     * Verifies the accepted state.
     */
    public void andVerify(Consumer<S> consumer) {
        consumer.accept(result.awaited());
    }

    /**
     * Verifies the accepted state and the result of the stimulus.
     */
    public void andVerify(BiConsumer<S, T> consumer) {
        consumer.accept(result.awaited(), result.stimulusResult());
    }

    /**
     * Verifies the events published so far in the test.
     */
    public void andVerifyEvents(Consumer<AssertablePublishedEvents> events) {
        events.accept(when.scenario().events());
    }

    /**
     * Also expects an event of the given type, which must have been published already.
     */
    public <E> EventResult<E, T> andExpect(Class<E> eventType) {
        return new EventResult<>(when, eventType, Function.identity(), result);
    }
}
