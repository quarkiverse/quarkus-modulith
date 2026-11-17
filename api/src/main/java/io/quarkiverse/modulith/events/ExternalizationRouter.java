package io.quarkiverse.modulith.events;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The immutable {@link EventExternalizationConfiguration.Router}, like Spring Modulith's {@code Router}: every method
 * returns a new router.
 */
final class ExternalizationRouter implements EventExternalizationConfiguration.Router {

    private final Predicate<Object> filter;
    private final Function<Object, Object> mapper;
    private final Function<Object, RoutingTarget> router;
    private final Function<Object, Map<String, Object>> headers;
    private final boolean serialize;

    private ExternalizationRouter(Predicate<Object> filter, Function<Object, Object> mapper,
            Function<Object, RoutingTarget> router, Function<Object, Map<String, Object>> headers, boolean serialize) {
        this.filter = filter;
        this.mapper = mapper;
        this.router = router;
        this.headers = headers;
        this.serialize = serialize;
    }

    /**
     * @return a router for the selected events that sends them unmapped, without headers, to the target of their
     *         {@code Externalized} annotation or else to their fully qualified type name
     */
    static ExternalizationRouter of(Predicate<Object> filter) {
        return new ExternalizationRouter(filter, Function.identity(),
                event -> ExternalizationSupport.mergeWithExternalizedAnnotation(event,
                        EventExternalizationConfiguration.byFullyQualifiedTypeName().apply(event)),
                event -> Map.of(), false);
    }

    @Override
    public EventExternalizationConfiguration.Router mapping(Function<Object, Object> mapping) {
        return new ExternalizationRouter(filter, Objects.requireNonNull(mapping, "mapping"), router, headers, serialize);
    }

    @Override
    public <T> EventExternalizationConfiguration.Router mapping(Class<T> type, Function<T, Object> mapping) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mapping, "mapping");
        Function<Object, Object> combined = event -> as(type, event).map(mapping).orElse(event);
        return new ExternalizationRouter(filter, mapper.compose(combined), router, headers, serialize);
    }

    @Override
    public EventExternalizationConfiguration.Router headers(Function<Object, Map<String, Object>> headers) {
        return new ExternalizationRouter(filter, mapper, router, Objects.requireNonNull(headers, "headers"), serialize);
    }

    @Override
    public <T> EventExternalizationConfiguration.Router headers(Class<T> type,
            Function<T, Map<String, Object>> headers) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(headers, "headers");
        Function<Object, Map<String, Object>> previous = this.headers;
        Function<Object, Map<String, Object>> combined = event -> as(type, event).map(headers)
                .orElseGet(() -> previous.apply(event));
        return new ExternalizationRouter(filter, mapper, router, combined, serialize);
    }

    @Override
    public <T> EventExternalizationConfiguration.Router route(Class<T> type, Function<T, RoutingTarget> router) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(router, "router");
        Function<Object, RoutingTarget> previous = this.router;
        return withRouter(event -> as(type, event).map(router).orElseGet(() -> previous.apply(event)));
    }

    @Override
    public <T> EventExternalizationConfiguration.Router routeKey(Class<T> type, Function<T, String> key) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        Function<Object, RoutingTarget> previous = this.router;
        return withRouter(event -> as(type, event)
                .map(typed -> previous.apply(typed).withKey(key.apply(typed)))
                .orElseGet(() -> previous.apply(event)));
    }

    @Override
    public EventExternalizationConfiguration.Router routeAll(Function<Object, RoutingTarget> router) {
        return withRouter(Objects.requireNonNull(router, "router"));
    }

    @Override
    public EventExternalizationConfiguration.Router routeAllByType(Function<Class<?>, RoutingTarget> router) {
        Objects.requireNonNull(router, "router");
        return withRouter(event -> router.apply(event.getClass()));
    }

    @Override
    public EventExternalizationConfiguration.Router routeMapped() {
        return withRouter(router.compose(mapper));
    }

    @Override
    public EventExternalizationConfiguration.Router routeMapped(BiFunction<Object, Object, RoutingTarget> router) {
        Objects.requireNonNull(router, "router");
        Function<Object, Object> mapping = this.mapper;
        return withRouter(event -> router.apply(event, mapping.apply(event)));
    }

    @Override
    public EventExternalizationConfiguration routeOptional(Function<Object, Optional<RoutingTarget>> router) {
        Objects.requireNonNull(router, "router");
        Function<Object, RoutingTarget> previous = this.router;
        return withRouter(event -> router.apply(event).orElseGet(() -> previous.apply(event))).build();
    }

    @Override
    public EventExternalizationConfiguration routeOptionalByType(Function<Class<?>, Optional<RoutingTarget>> router) {
        Objects.requireNonNull(router, "router");
        return routeOptional(event -> router.apply(event.getClass()));
    }

    @Override
    public EventExternalizationConfiguration.Router serializeExternalization() {
        return serializeExternalization(true);
    }

    @Override
    public EventExternalizationConfiguration.Router serializeExternalization(boolean serialize) {
        return new ExternalizationRouter(filter, mapper, router, headers, serialize);
    }

    @Override
    public EventExternalizationConfiguration build() {
        return new DefaultEventExternalizationConfiguration(filter, mapper, router, headers, serialize);
    }

    private ExternalizationRouter withRouter(Function<Object, RoutingTarget> router) {
        return new ExternalizationRouter(filter, mapper, router, headers, serialize);
    }

    private static <T> Optional<T> as(Class<T> type, Object event) {
        return Optional.of(event).filter(type::isInstance).map(type::cast);
    }
}
