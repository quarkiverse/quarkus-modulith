package io.quarkiverse.modulith.events;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The {@link EventExternalizationConfiguration.Selector} returned by
 * {@link EventExternalizationConfiguration#externalizing()}, like Spring Modulith's {@code Selector}.
 */
final class ExternalizationSelector implements EventExternalizationConfiguration.Selector {

    @Override
    public EventExternalizationConfiguration.Router select(Predicate<Object> filter) {
        return ExternalizationRouter.of(Objects.requireNonNull(filter, "filter"));
    }

    @Override
    public EventExternalizationConfiguration.Router selectByPackage(String basePackage) {
        if (Objects.isNull(basePackage) || basePackage.isBlank()) {
            throw new IllegalArgumentException("Base package must not be blank");
        }
        return select(event -> ExternalizationSupport.residesIn(event.getClass(), basePackage));
    }

    @Override
    public EventExternalizationConfiguration.Router selectByPackage(Class<?> type) {
        return selectByPackage(Objects.requireNonNull(type, "type").getPackageName());
    }

    @Override
    public EventExternalizationConfiguration.Router selectByPackagesAndFilter(Collection<String> basePackages,
            Predicate<Object> filter) {
        if (Objects.isNull(basePackages) || basePackages.isEmpty()) {
            throw new IllegalArgumentException("Base packages must not be empty");
        }
        Objects.requireNonNull(filter, "filter");
        List<String> packages = List.copyOf(basePackages);
        Predicate<Object> residesInPackage = event -> packages.stream()
                .anyMatch(candidate -> ExternalizationSupport.residesIn(event.getClass(), candidate));
        return select(residesInPackage.and(filter));
    }

    @Override
    public EventExternalizationConfiguration.Router selectByType(Class<?> type) {
        return select(Objects.requireNonNull(type, "type")::isInstance);
    }

    @Override
    public EventExternalizationConfiguration.Router selectByType(Predicate<Class<?>> filter) {
        Objects.requireNonNull(filter, "filter");
        return select(event -> filter.test(event.getClass()));
    }

    @Override
    public EventExternalizationConfiguration.Router selectByAnnotation(Class<? extends Annotation> annotation) {
        Objects.requireNonNull(annotation, "annotation");
        return select(event -> AnnotationLookup.find(event.getClass(), annotation).isPresent());
    }

    @Override
    public <T extends Annotation> EventExternalizationConfiguration.Router selectAndRoute(Class<T> annotation,
            Function<T, String> router) {
        Objects.requireNonNull(router, "router");
        return selectAndRoute(annotation, (event, found) -> ParsedRoutingTarget.parse(router.apply(found))
                .withFallback(EventExternalizationConfiguration.byFullyQualifiedTypeName().apply(event)));
    }

    @Override
    public <T extends Annotation> EventExternalizationConfiguration.Router selectAndRoute(Class<T> annotation,
            BiFunction<Object, T, RoutingTarget> router) {
        Objects.requireNonNull(router, "router");
        return selectByAnnotation(annotation)
                .routeAll(event -> router.apply(event, AnnotationLookup.find(event.getClass(), annotation)
                        .orElseThrow(() -> new IllegalStateException("No annotation " + annotation.getName()
                                + " on type " + event.getClass().getName()))));
    }

    @Override
    public EventExternalizationConfiguration.Router annotatedAsExternalized() {
        return select(EventExternalizationConfiguration.annotatedAsExternalized());
    }
}
