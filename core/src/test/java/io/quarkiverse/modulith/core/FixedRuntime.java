package io.quarkiverse.modulith.core;

import java.util.Optional;

/**
 * A runtime that considers {@code java.lang} the application.
 */
class FixedRuntime implements ApplicationRuntime {

    @Override
    public Optional<String> getId() {
        return Optional.of("fixed");
    }

    @Override
    public Optional<Class<?>> getMainApplicationClass() {
        return Optional.empty();
    }

    @Override
    public Class<?> getUserClass(Object bean, String beanName) {
        return bean.getClass();
    }

    @Override
    public boolean isApplicationClass(Class<?> type) {
        return type.getPackageName().equals("java.lang");
    }
}
