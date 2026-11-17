package io.quarkiverse.modulith.deployment.fixtures.events.composed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * Carries the listener annotation directly and through {@link SyncListener}, so discovery reaches it twice.
 */
@ApplicationModuleListener(async = false)
@SyncListener
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DoubleListener {
}
