package io.quarkiverse.modulith.test;

import static java.util.Objects.isNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An event a {@link Scenario} expects. Spring Modulith declares it as {@code Scenario.When.EventResult}.
 *
 * @param <E> the event type
 * @param <T> the result of the stimulus
 */
public final class EventResult<E, T> {

    private final When<T> when;
    private final Class<E> type;
    private final Function<TypedPublishedEvents<E>, TypedPublishedEvents<E>> filter;
    private final ExecutionResult<?, T> previousResult;

    EventResult(When<T> when, Class<E> type, Function<TypedPublishedEvents<E>, TypedPublishedEvents<E>> filter,
            ExecutionResult<?, T> previousResult) {
        this.when = when;
        this.type = type;
        this.filter = filter;
        this.previousResult = previousResult;
    }

    /**
     * Only accepts events matching the predicate.
     */
    public EventResult<E, T> matching(Predicate<? super E> predicate) {
        return with(it -> it.matching(predicate));
    }

    /**
     * Only accepts events whose value extracted by the function matches the predicate.
     */
    public <S> EventResult<E, T> matchingMapped(Function<E, S> extractor, Predicate<? super S> predicate) {
        return with(it -> it.matching(extractor, predicate));
    }

    /**
     * Only accepts events whose value extracted by the function equals the given value.
     */
    public <S> EventResult<E, T> matchingMappedValue(Function<E, S> extractor, S value) {
        return with(it -> it.matchingValue(extractor, value));
    }

    /**
     * Waits for a matching event.
     *
     * @return the first matching event
     */
    public E toArrive() {
        toArriveAndVerifyInternal(result -> {
        });
        return firstEvent();
    }

    /**
     * Waits for a matching event and verifies the first one.
     */
    public void toArriveAndVerify(Consumer<E> consumer) {
        toArriveAndVerifyInternal(result -> consumer.accept(firstEvent()));
    }

    /**
     * Waits for a matching event and verifies the first one together with the result of the stimulus.
     */
    public void toArriveAndVerify(BiConsumer<E, T> consumer) {
        toArriveAndVerifyInternal(result -> consumer.accept(firstEvent(), result));
    }

    /**
     * Waits for a matching event and runs AssertJ assertions on the matching events.
     */
    public void toArriveAndAssert(Consumer<PublishedEventAssert<? super E>> consumer) {
        toArriveAndVerifyInternal(result -> consumer.accept(assertedEvent()));
    }

    /**
     * Waits for a matching event and runs AssertJ assertions on the matching events, with the result of the
     * stimulus.
     */
    public void toArriveAndAssert(BiConsumer<PublishedEventAssert<? super E>, T> consumer) {
        toArriveAndVerifyInternal(result -> consumer.accept(assertedEvent(), result));
    }

    private EventResult<E, T> with(Function<TypedPublishedEvents<E>, TypedPublishedEvents<E>> next) {
        return new EventResult<>(when, type, filter.andThen(next), previousResult);
    }

    private TypedPublishedEvents<E> filteredEvents() {
        return filter.apply(when.scenario().events().ofType(type));
    }

    private E firstEvent() {
        return filteredEvents().iterator().next();
    }

    private PublishedEventAssert<E> assertedEvent() {
        return new PublishedEventsAssert(filteredEvents()).contains(type);
    }

    private void toArriveAndVerifyInternal(Consumer<T> verifications) {
        if (isNull(previousResult)) {
            when.awaitInternal(verifications, this::filteredEvents, it -> it.eventOfTypeWasPublished(type));
            return;
        }
        if (!filteredEvents().eventOfTypeWasPublished(type)) {
            throw new AssertionError(("Expected an event of type %s (potentially further constrained using matching "
                    + "clauses above) to be published but couldn't find one in %s").formatted(type.getName(),
                            when.scenario().events()));
        }
        verifications.accept(previousResult.stimulusResult());
    }
}
