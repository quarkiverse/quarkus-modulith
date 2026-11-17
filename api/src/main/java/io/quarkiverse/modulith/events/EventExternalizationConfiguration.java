package io.quarkiverse.modulith.events;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Decides which events are externalized, where they go and what is sent, like Spring Modulith's
 * {@code EventExternalizationConfiguration}. Declare a CDI bean of this type to replace the default, which selects the
 * {@link Externalized} types below the application's base package and routes them by their application-local name.
 */
public interface EventExternalizationConfiguration {

    /**
     * @return whether the event is externalized
     */
    boolean supports(Object event);

    /**
     * @return where the event goes. Target and key may still be expressions, which the externalizer resolves against
     *         the original event.
     */
    RoutingTarget determineTarget(Object event);

    /**
     * @return the payload to send for the event
     */
    Object map(Object event);

    /**
     * @return the headers to send with the event, never {@code null}
     */
    Map<String, Object> getHeadersFor(Object event);

    /**
     * @return whether events are sent one at a time, keeping their order
     */
    boolean serializeExternalization();

    /**
     * Starts a configuration: select the events first, then route them.
     */
    static Selector externalizing() {
        return new ExternalizationSelector();
    }

    /**
     * The default configuration: only events of types below the given packages that carry a supported
     * {@code Externalized} annotation, routed by the annotation or else by their application-local name, so that
     * {@code com.acme.order.OrderPlaced} goes to {@code order.OrderPlaced} for the package {@code com.acme}.
     *
     * @param packages must not be empty
     */
    static Router defaults(Collection<String> packages) {
        if (Objects.isNull(packages) || packages.isEmpty()) {
            throw new IllegalArgumentException("Packages must not be empty");
        }
        List<String> copy = List.copyOf(packages);
        Function<Object, RoutingTarget> router = event -> byApplicationLocalName(copy).apply(event)
                .map(target -> ExternalizationSupport.mergeWithExternalizedAnnotation(event, target))
                .orElseGet(() -> byFullyQualifiedTypeName().apply(event));
        return externalizing()
                .selectByPackagesAndFilter(copy, annotatedAsExternalized())
                .routeAll(router);
    }

    /**
     * @see #defaults(Collection)
     */
    static Router defaults(String... packages) {
        return defaults(List.of(packages));
    }

    /**
     * @return a configuration that externalizes nothing
     */
    static EventExternalizationConfiguration disabled() {
        return externalizing().select(event -> false).build();
    }

    /**
     * @return a predicate selecting events whose type carries a supported {@code Externalized} annotation: Quarkus
     *         Modulith's {@link Externalized} or jMolecules' {@code org.jmolecules.event.annotation.Externalized}
     */
    static Predicate<Object> annotatedAsExternalized() {
        return AnnotationTargetLookup::hasExternalizedAnnotation;
    }

    /**
     * @return a routing that uses the type name relative to the first of the packages that contains the type, empty
     *         for types outside of them
     */
    static Function<Object, Optional<RoutingTarget>> byApplicationLocalName(Collection<String> packages) {
        List<String> copy = List.copyOf(Objects.requireNonNull(packages, "packages"));
        return event -> {
            Class<?> type = event.getClass();
            return copy.stream()
                    .filter(candidate -> ExternalizationSupport.residesIn(type, candidate))
                    .findFirst()
                    .map(candidate -> RoutingTarget.forTarget(type.getName().substring(candidate.length() + 1)));
        };
    }

    /**
     * @return a lookup of the routing target declared by the supported {@code Externalized} annotations
     */
    static Function<Object, Optional<ParsedRoutingTarget>> byExternalizedAnnotations() {
        return event -> AnnotationTargetLookup.targetOf(event.getClass());
    }

    /**
     * @return a routing that uses the fully qualified type name of the event
     */
    static Function<Object, RoutingTarget> byFullyQualifiedTypeName() {
        return event -> RoutingTarget.forTarget(event.getClass().getName());
    }

    /**
     * Selects the events to externalize.
     */
    interface Selector {

        /**
         * @return a router for the events matching the predicate
         */
        Router select(Predicate<Object> filter);

        /**
         * @return a router for the events whose type is in the package or a sub-package
         */
        Router selectByPackage(String basePackage);

        /**
         * @return a router for the events whose type is in the package of the given type or a sub-package
         */
        Router selectByPackage(Class<?> type);

        /**
         * @return a router for the events below one of the packages that also match the filter
         */
        Router selectByPackagesAndFilter(Collection<String> basePackages, Predicate<Object> filter);

        /**
         * @return a router for the events that are instances of the type
         */
        Router selectByType(Class<?> type);

        /**
         * @return a router for the events whose type matches the predicate
         */
        Router selectByType(Predicate<Class<?>> filter);

        /**
         * @return a router for the events whose type carries the annotation, directly, on a supertype or as a
         *         meta-annotation
         */
        Router selectByAnnotation(Class<? extends Annotation> annotation);

        /**
         * Selects by annotation and routes by the {@code target} or {@code target::key} the function reads from it.
         * A missing target falls back to the fully qualified type name.
         */
        <T extends Annotation> Router selectAndRoute(Class<T> annotation, Function<T, String> router);

        /**
         * Selects by annotation and routes with a function of the event and the annotation.
         */
        <T extends Annotation> Router selectAndRoute(Class<T> annotation, BiFunction<Object, T, RoutingTarget> router);

        /**
         * @return a router for the events whose type carries a supported {@code Externalized} annotation
         */
        Router annotatedAsExternalized();
    }

    /**
     * Maps and routes the selected events. Every method returns a new router.
     */
    interface Router {

        /**
         * Replaces the mapping of every event.
         */
        Router mapping(Function<Object, Object> mapping);

        /**
         * Maps the events of the given type, before the mappings registered so far, on the original event. The result
         * then goes through the earlier mappings, like in Spring Modulith.
         */
        <T> Router mapping(Class<T> type, Function<T, Object> mapping);

        /**
         * Replaces the headers of every event. Headers are computed from the mapped payload.
         */
        Router headers(Function<Object, Map<String, Object>> headers);

        /**
         * Sets the headers of the payloads of the given type, the others keep the headers registered so far.
         */
        <T> Router headers(Class<T> type, Function<T, Map<String, Object>> headers);

        /**
         * Routes the events of the given type with the function, the others keep the routing registered so far.
         */
        <T> Router route(Class<T> type, Function<T, RoutingTarget> router);

        /**
         * Sets the key of the events of the given type, keeping the target the routing registered so far gives.
         */
        <T> Router routeKey(Class<T> type, Function<T, String> key);

        /**
         * Routes every event with the function.
         */
        Router routeAll(Function<Object, RoutingTarget> router);

        /**
         * Routes every event by its type.
         */
        Router routeAllByType(Function<Class<?>, RoutingTarget> router);

        /**
         * Routes the mapped payload instead of the original event.
         */
        Router routeMapped();

        /**
         * Routes the original event and the mapped payload with the function.
         */
        Router routeMapped(BiFunction<Object, Object, RoutingTarget> router);

        /**
         * Routes by the function, falling back to the routing registered so far when it returns an empty optional.
         */
        EventExternalizationConfiguration routeOptional(Function<Object, Optional<RoutingTarget>> router);

        /**
         * Routes by the event type, falling back to the routing registered so far when it returns an empty optional.
         */
        EventExternalizationConfiguration routeOptionalByType(Function<Class<?>, Optional<RoutingTarget>> router);

        /**
         * Sends events one at a time, keeping their order.
         */
        Router serializeExternalization();

        /**
         * Sets whether events are sent one at a time.
         */
        Router serializeExternalization(boolean serialize);

        /**
         * @return the configuration
         */
        EventExternalizationConfiguration build();
    }
}
