package io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.quarkiverse.modulith.events.Externalized;

@Documented
@Externalized("meta::#{#this.id()}")
@Retention(RetentionPolicy.RUNTIME)
public @interface Meta {
}
