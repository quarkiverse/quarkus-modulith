package io.quarkiverse.modulith.test;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;

import org.awaitility.core.ConditionFactory;

/**
 * A test of module interactions: a stimulus, the event or state change to wait for, and the verification. The
 * Quarkus counterpart of Spring Modulith's {@code Scenario}. Declare a parameter of this type in a test method of an
 * {@link ApplicationModuleTest} or an {@link EnableScenarios} class:
 *
 * <pre>
 * &#64;Test
 * void reservesStock(Scenario scenario) {
 *     scenario.publish(new OrderPlaced("o-1", "sku-1", 5))
 *             .andWaitForStateChange(() -&gt; inventory.stockOf("sku-1"), stock -&gt; stock == 95)
 *             .andVerify(stock -&gt; assertThat(stock).isEqualTo(95));
 * }
 * </pre>
 *
 * The stimulus runs in a new transaction (see {@link TransactionOperations}), so module listeners, which run after
 * the commit, see it. Waiting uses Awaitility, which polls for ten seconds unless customized with
 * {@link When#andWaitAtMost(java.time.Duration)}, {@link When#customize(Function)} or a {@link ScenarioCustomizer}.
 */
public final class Scenario {

    private static final Predicate<Object> DEFAULT_ACCEPTANCE = Scenario::isChanged;

    private final TransactionOperations transactions;
    private final Event<Object> publisher;
    private final AssertablePublishedEvents events;
    private Function<ConditionFactory, ConditionFactory> defaultCustomizer = Function.identity();

    Scenario(TransactionOperations transactions, Event<Object> publisher, AssertablePublishedEvents events) {
        this.transactions = transactions;
        this.publisher = publisher;
        this.events = events;
    }

    /**
     * Publishes the event in a new transaction.
     */
    public When<Void> publish(Object event) {
        return publish(() -> event);
    }

    /**
     * Publishes the event the supplier returns, in a new transaction.
     */
    public When<Void> publish(Supplier<Object> event) {
        return when((tx, publisher) -> {
            tx.executeWithoutResult(() -> publisher.fire(event.get()));
            return null;
        });
    }

    /**
     * Runs the stimulus in a new transaction.
     */
    public When<Void> stimulate(Runnable runnable) {
        return stimulate(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs the stimulus in a new transaction. Its result is passed to the verification and cleanup callbacks that take
     * it.
     */
    public <S> When<S> stimulate(Supplier<S> supplier) {
        return stimulate(tx -> tx.execute(supplier));
    }

    /**
     * Runs the stimulus, which controls transactions itself.
     */
    public <S> When<S> stimulate(Function<TransactionOperations, S> function) {
        return when((tx, publisher) -> function.apply(tx));
    }

    /**
     * Runs the stimulus, which controls transactions itself and may publish events.
     */
    public When<Void> stimulate(BiConsumer<TransactionOperations, Event<Object>> stimulus) {
        return when((tx, publisher) -> {
            stimulus.accept(tx, publisher);
            return null;
        });
    }

    /**
     * Runs the stimulus, which controls transactions itself and may publish events.
     */
    public <S> When<S> stimulate(BiFunction<TransactionOperations, Event<Object>, S> stimulus) {
        return when(stimulus);
    }

    private <S> When<S> when(BiFunction<TransactionOperations, Event<Object>, S> stimulus) {
        return new When<>(this, stimulus, result -> {
        }, defaultCustomizer);
    }

    /**
     * Sets the customizer every {@link When} of this scenario starts from. Used by {@link ScenarioCustomizer}.
     */
    Scenario setDefaultCustomizer(Function<ConditionFactory, ConditionFactory> customizer) {
        this.defaultCustomizer = customizer;
        return this;
    }

    Function<ConditionFactory, ConditionFactory> defaultCustomizer() {
        return defaultCustomizer;
    }

    TransactionOperations transactions() {
        return transactions;
    }

    Event<Object> publisher() {
        return publisher;
    }

    AssertablePublishedEvents events() {
        return events;
    }

    @SuppressWarnings("unchecked")
    static <S> Predicate<? super S> defaultAcceptance() {
        return (Predicate<? super S>) DEFAULT_ACCEPTANCE;
    }

    /**
     * @return whether a polled value counts as a state change: not {@code null}, not {@code false}, not an empty
     *         collection or optional
     */
    static boolean isChanged(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.isPresent();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Objects.nonNull(value);
    }
}
