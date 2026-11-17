package io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.quarkiverse.modulith.events.Externalized;

@Meta
@Externalized
@Retention(RetentionPolicy.RUNTIME)
public @interface DoubleMeta {
}
