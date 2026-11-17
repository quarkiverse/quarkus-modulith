package io.quarkiverse.modulith.observability.runtime;

import io.opentelemetry.context.ContextKey;

/**
 * Carries the identifier of the current module in the OpenTelemetry context.
 */
final class ModuleContextKey {

    static final ContextKey<String> KEY = ContextKey.named("modulith.module.identifier");

    private ModuleContextKey() {
    }
}
