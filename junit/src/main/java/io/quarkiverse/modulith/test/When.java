package io.quarkiverse.modulith.test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

/**
 * A stimulus of a {@link Scenario} and how to wait for its outcome. Spring Modulith declares it as
 * {@code Scenario.When}. The stimulus runs when an {@code andWaitFor…}/{@code for…} result is evaluated.
 *
 * @param <T> the result of the stimulus
 */
public final class When<T> {

    private final Scenario scenario;
    private final BiFunction<TransactionOperations, Event<Object>, T> stimulus;
    private final Consumer<T> cleanup;
    private final Function<ConditionFactory, ConditionFactory> customizer;

    When(Scenario scenario, BiFunction<TransactionOperations, Event<Object>, T> stimulus, Consumer<T> cleanup,
            Function<ConditionFactory, ConditionFactory> customizer) {
        this.scenario = scenario;
        this.stimulus = stimulus;
        this.cleanup = cleanup;
        this.customizer = customizer;
    }

    /**
     * Runs the callback after the scenario, also when it fails.
     */
    public When<T> andCleanup(Runnable runnable) {
        return andCleanup(result -> runnable.run());
    }

    /**
     * Runs the callback with the stimulus result after the scenario, also when it fails.
     */
    public When<T> andCleanup(Consumer<T> consumer) {
        return new When<>(scenario, stimulus, consumer, customizer);
    }

    /**
     * Waits at most the given time for the expected event or state change.
     */
    public When<T> andWaitAtMost(Duration duration) {
        return customize(it -> it.atMost(duration));
    }

    /**
     * Customizes the Awaitility condition used to wait, on top of the customizations so far.
     */
    public When<T> customize(Function<ConditionFactory, ConditionFactory> customizer) {
        return new When<>(scenario, stimulus, cleanup, this.customizer.andThen(customizer));
    }

    /**
     * Same as {@link #andWaitForEventOfType(Class)}.
     */
    public <E> EventResult<E, T> forEventOfType(Class<E> type) {
        return andWaitForEventOfType(type);
    }

    /**
     * Same as {@link #andWaitForStateChange(Supplier)}.
     */
    public <S> StateChangeResult<S, T> forStateChange(Supplier<S> supplier) {
        return andWaitForStateChange(supplier);
    }

    /**
     * Same as {@link #andWaitForStateChange(Supplier, Predicate)}.
     */
    public <S> StateChangeResult<S, T> forStateChange(Supplier<S> supplier, Predicate<? super S> acceptanceCriteria) {
        return andWaitForStateChange(supplier, acceptanceCriteria);
    }

    /**
     * Expects an event of the given type to be published, possibly constrained further with
     * {@link EventResult#matching(Predicate)}.
     */
    public <E> EventResult<E, T> andWaitForEventOfType(Class<E> type) {
        return new EventResult<>(this, type, Function.identity(), null);
    }

    /**
     * Waits until the supplier returns a value that is not {@code null}, {@code false}, or an empty collection or
     * optional.
     */
    public <S> StateChangeResult<S, T> andWaitForStateChange(Supplier<S> supplier) {
        return andWaitForStateChange(supplier, Scenario.defaultAcceptance());
    }

    /**
     * Waits until the supplier returns a value the acceptance criteria accept.
     */
    public <S> StateChangeResult<S, T> andWaitForStateChange(Supplier<S> supplier,
            Predicate<? super S> acceptanceCriteria) {
        return new StateChangeResult<>(this, awaitInternal(result -> {
        }, supplier::get, acceptanceCriteria));
    }

    /**
     * Runs the stimulus, waits for the condition, runs the verifications and finally the cleanup.
     */
    <S> ExecutionResult<S, T> awaitInternal(Consumer<T> verifications, Callable<S> supplier,
            Predicate<? super S> condition) {
        T result = stimulus.apply(scenario.transactions(), scenario.publisher());
        try {
            S awaited = customizer.apply(Awaitility.await()).until(supplier, condition::test);
            verifications.accept(result);
            return new ExecutionResult<>(awaited, result);
        } finally {
            cleanup.accept(result);
        }
    }

    Scenario scenario() {
        return scenario;
    }
}
