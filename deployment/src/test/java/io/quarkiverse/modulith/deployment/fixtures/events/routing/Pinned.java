package io.quarkiverse.modulith.deployment.fixtures.events.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Scope;

/**
 * A custom pseudo-scope the dispatcher knows nothing about.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Pinned {
}
