package io.quarkiverse.modulith.test;

import java.lang.annotation.Annotation;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Singleton;

/**
 * Why a class of a module is a bean, following ArC's discovery rules.
 */
enum ComponentKind {

    /**
     * Not a bean.
     */
    NONE(null),

    /**
     * Declares a scope or a stereotype.
     */
    DECLARED(null),

    /**
     * Declares {@code @ApplicationModuleListener} methods without a scope. The extension makes it a singleton.
     */
    LISTENER(Singleton.class),

    /**
     * Declares producer or observer members without a scope. ArC makes it a dependent bean.
     */
    PRODUCER_OR_OBSERVER(Dependent.class);

    private final Class<? extends Annotation> impliedScope;

    ComponentKind(Class<? extends Annotation> impliedScope) {
        this.impliedScope = impliedScope;
    }

    /**
     * @return the scope the application would give the class, empty if it declares one or is no bean
     */
    Optional<Class<? extends Annotation>> impliedScope() {
        return Optional.ofNullable(impliedScope);
    }
}
