package io.quarkiverse.modulith.observability.runtime;

import java.util.Objects;

import jakarta.inject.Singleton;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkiverse.modulith.observability.ModulithObservations;

/**
 * Tags every span started inside a module invocation with the module identifier, Spring Modulith's
 * {@code ModuleIdentifierPassingObservationFilter}.
 */
@Singleton
public class ModuleIdentifierSpanProcessor implements SpanProcessor {

    static final AttributeKey<String> MODULE_IDENTIFIER = AttributeKey.stringKey(ModulithObservations.MODULE_IDENTIFIER);

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String module = parentContext.get(ModuleContextKey.KEY);
        if (Objects.nonNull(module) && Objects.isNull(span.getAttribute(MODULE_IDENTIFIER))) {
            span.setAttribute(MODULE_IDENTIFIER, module);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
