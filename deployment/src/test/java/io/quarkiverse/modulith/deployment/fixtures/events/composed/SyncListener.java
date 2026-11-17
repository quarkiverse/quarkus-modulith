package io.quarkiverse.modulith.deployment.fixtures.events.composed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkiverse.modulith.ApplicationModuleListener;

@ApplicationModuleListener(async = false)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface SyncListener {
}
