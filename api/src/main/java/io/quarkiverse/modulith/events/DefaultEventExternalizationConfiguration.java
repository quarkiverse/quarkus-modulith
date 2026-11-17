package io.quarkiverse.modulith.events;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The configuration the {@link EventExternalizationConfiguration.Router} builds, like Spring Modulith's
 * {@code DefaultEventExternalizationConfiguration}.
 */
final class DefaultEventExternalizationConfiguration implements EventExternalizationConfiguration {

    private final Predicate<Object> filter;
    private final Function<Object, Object> mapper;
    private final Function<Object, RoutingTarget> router;
    private final Function<Object, Map<String, Object>> headers;
    private final boolean serialize;

    DefaultEventExternalizationConfiguration(Predicate<Object> filter, Function<Object, Object> mapper,
            Function<Object, RoutingTarget> router, Function<Object, Map<String, Object>> headers, boolean serialize) {
        this.filter = filter;
        this.mapper = mapper;
        this.router = router;
        this.headers = headers;
        this.serialize = serialize;
    }

    @Override
    public boolean supports(Object event) {
        return filter.test(Objects.requireNonNull(event, "event"));
    }

    @Override
    public Object map(Object event) {
        return mapper.apply(Objects.requireNonNull(event, "event"));
    }

    @Override
    public RoutingTarget determineTarget(Object event) {
        RoutingTarget target = router.apply(Objects.requireNonNull(event, "event"));
        if (Objects.isNull(target)) {
            throw new IllegalStateException("No routing target for an event of type " + event.getClass().getName()
                    + ". Make sure the externalization configuration always produces a target");
        }
        return target;
    }

    @Override
    public Map<String, Object> getHeadersFor(Object event) {
        return Objects.requireNonNullElse(headers.apply(Objects.requireNonNull(event, "event")), Map.of());
    }

    @Override
    public boolean serializeExternalization() {
        return serialize;
    }
}
