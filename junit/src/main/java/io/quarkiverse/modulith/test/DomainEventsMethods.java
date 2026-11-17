package io.quarkiverse.modulith.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds and remembers the {@code @DomainEvents} method of each aggregate class. As a {@link ClassValue} it does not
 * keep the aggregate classes, and so their class loaders, from being collected.
 */
final class DomainEventsMethods extends ClassValue<Optional<Method>> {

    private static final String ANNOTATION_NAME = "DomainEvents";

    @Override
    protected Optional<Method> computeValue(Class<?> type) {
        return Stream.<Class<?>> iterate(type, it -> it != Object.class, Class::getSuperclass)
                .flatMap(it -> Arrays.stream(it.getDeclaredMethods()))
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> Collection.class.isAssignableFrom(method.getReturnType()))
                .filter(DomainEventsMethods::isAnnotated)
                .findFirst();
    }

    private static boolean isAnnotated(Method method) {
        return Arrays.stream(method.getAnnotations()).map(Annotation::annotationType)
                .anyMatch(type -> type.getSimpleName().equals(ANNOTATION_NAME));
    }
}
