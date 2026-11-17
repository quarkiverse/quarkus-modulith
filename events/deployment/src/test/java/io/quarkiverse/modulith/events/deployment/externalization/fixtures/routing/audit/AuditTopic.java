package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("meta::{event.kind}")
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditTopic {
}
