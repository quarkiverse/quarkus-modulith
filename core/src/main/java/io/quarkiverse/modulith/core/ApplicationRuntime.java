package io.quarkiverse.modulith.core;

import java.util.Optional;

/**
 * The running application, like Spring Modulith's {@code ApplicationRuntime}. Quarkus Modulith provides it as a bean.
 */
public interface ApplicationRuntime {

    /**
     * @return the application name, {@code quarkus.application.name}
     */
    Optional<String> getId();

    /**
     * @return the type annotated with {@code @Modulith} or {@code @Modulithic}, if any
     */
    Optional<Class<?>> getMainApplicationClass();

    /**
     * @return the type the bean was declared with, without the client proxy or interception subclass Quarkus generates
     */
    Class<?> getUserClass(Object bean, String beanName);

    /**
     * @return whether the type belongs to the application, that is lives in one of its root packages
     */
    boolean isApplicationClass(Class<?> type);
}
