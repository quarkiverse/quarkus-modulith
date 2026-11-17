package io.quarkiverse.modulith.test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Reads the domain events an aggregate collected, without publishing them. The Quarkus counterpart of Spring
 * Modulith's {@code AggregateTestUtils}.
 * <p>
 * The aggregate exposes its events through a method without parameters that returns a {@link Collection} and carries
 * an annotation named {@code DomainEvents}, such as Spring Data's {@code @DomainEvents} or an application's own
 * annotation of that name. The method is looked up on the class and its superclasses.
 */
public final class AggregateTestUtils {

    private static final DomainEventsMethods METHODS = new DomainEventsMethods();

    private AggregateTestUtils() {
    }

    /**
     * @param aggregate the aggregate, must not be {@code null}
     * @return the domain events of the aggregate, empty if it has no {@code @DomainEvents} method
     * @throws IllegalStateException if the method cannot be called or fails
     */
    public static PublishedEvents eventsOf(Object aggregate) {
        Objects.requireNonNull(aggregate, "aggregate");
        return METHODS.get(aggregate.getClass())
                .map(method -> invoke(method, aggregate))
                .map(PublishedEvents::of)
                .orElseGet(() -> PublishedEvents.of(List.of()));
    }

    static Collection<?> invoke(Method method, Object aggregate) {
        try {
            method.setAccessible(true);
            return (Collection<?>) method.invoke(aggregate);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot read the domain events of " + aggregate.getClass().getName(), e);
        }
    }
}
