package io.quarkiverse.modulith.observability.runtime;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * The span of a module invocation and the context scope that makes it current.
 */
record SpanScope(Span span, Scope scope) implements ObservationScope {

    @Override
    public void error(Throwable error) {
        span.recordException(error);
        span.setStatus(StatusCode.ERROR);
    }

    @Override
    public void stop() {
        scope.close();
        span.end();
    }
}
