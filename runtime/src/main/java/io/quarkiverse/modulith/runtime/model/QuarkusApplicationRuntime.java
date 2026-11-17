package io.quarkiverse.modulith.runtime.model;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.core.ApplicationRuntime;

/**
 * The running Quarkus application, with values recorded at build time.
 *
 * @param id the application name, empty if none
 * @param mainClass the name of the type annotated with {@code @Modulith}, empty if none
 * @param rootPackages the root packages of the module model
 */
public record QuarkusApplicationRuntime(String id, String mainClass, List<String> rootPackages)
        implements
            ApplicationRuntime {

    static final List<String> GENERATED_SUFFIXES = List.of("_ClientProxy", "_Subclass");

    @Override
    public Optional<String> getId() {
        return Optional.of(id).filter(it -> !it.isEmpty());
    }

    /**
     * @throws IllegalStateException if the recorded class cannot be loaded
     */
    @Override
    public Optional<Class<?>> getMainApplicationClass() {
        return Optional.of(mainClass).filter(it -> !it.isEmpty()).map(QuarkusApplicationRuntime::load);
    }

    static Class<?> load(String name) {
        try {
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load the application class " + name, e);
        }
    }

    @Override
    public Class<?> getUserClass(Object bean, String beanName) {
        Class<?> type = bean.getClass();
        while (GENERATED_SUFFIXES.stream().anyMatch(type.getName()::endsWith)) {
            type = type.getSuperclass();
        }
        return type;
    }

    @Override
    public boolean isApplicationClass(Class<?> type) {
        String name = type.getName();
        return rootPackages.stream().anyMatch(it -> name.startsWith(it + "."));
    }
}
