package io.quarkiverse.modulith.observability;

import io.opentelemetry.api.trace.SpanBuilder;

/**
 * Customizes the spans of module invocations, the Quarkus counterpart of Spring Modulith's
 * {@code ModulithObservationConvention}. Implementations are CDI beans; the span already carries the default name and
 * attributes when they are called.
 */
public interface ModulithSpanCustomizer {

    /**
     * @param invocation the module invocation the span is created for
     * @param span the span, not started yet
     */
    void customize(ModuleInvocation invocation, SpanBuilder span);
}
