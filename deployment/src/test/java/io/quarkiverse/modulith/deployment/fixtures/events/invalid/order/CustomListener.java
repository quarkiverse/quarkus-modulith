package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * A composed listener annotation that no method uses.
 */
@ApplicationModuleListener
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomListener {
}
