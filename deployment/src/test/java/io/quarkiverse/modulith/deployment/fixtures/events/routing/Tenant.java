package io.quarkiverse.modulith.deployment.fixtures.events.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.NormalScope;

/**
 * A custom scope the dispatcher knows nothing about.
 */
@NormalScope
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Tenant {
}
