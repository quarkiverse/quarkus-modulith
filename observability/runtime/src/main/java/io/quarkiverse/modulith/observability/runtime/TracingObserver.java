package io.quarkiverse.modulith.observability.runtime;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkiverse.modulith.observability.ModuleInvocation;
import io.quarkiverse.modulith.observability.ModulithObservations;
import io.quarkiverse.modulith.observability.ModulithSpanCustomizer;
import io.quarkus.arc.All;

/**
 * Opens an OpenTelemetry span per module invocation, named and tagged like Spring Modulith's observation.
 */
@Singleton
public class TracingObserver implements InvocationObserver {

    @Inject
    Tracer tracer;

    @Inject
    @All
    List<ModulithSpanCustomizer> customizers;

    @Override
    public ObservationScope start(ModuleInvocation invocation) {
        SpanBuilder builder = tracer.spanBuilder(invocation.contextualName())
                .setParent(Context.current())
                .setAttribute(ModulithObservations.MODULE_IDENTIFIER, invocation.moduleIdentifier())
                .setAttribute(ModulithObservations.MODULE_NAME, invocation.moduleName())
                .setAttribute(ModulithObservations.INVOCATION_TYPE, invocation.invocationType())
                .setAttribute(ModulithObservations.MODULE_METHOD, invocation.method());
        customizers.forEach(it -> it.customize(invocation, builder));
        Span span = builder.startSpan();
        Scope scope = Context.current().with(span).with(ModuleContextKey.KEY, invocation.moduleIdentifier())
                .makeCurrent();
        return new SpanScope(span, scope);
    }
}
