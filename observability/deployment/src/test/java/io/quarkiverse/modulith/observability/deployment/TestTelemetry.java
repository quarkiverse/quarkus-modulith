package io.quarkiverse.modulith.observability.deployment;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

/**
 * Collects the spans and meters of the test application in memory.
 */
public class TestTelemetry {

    static final InMemorySpanExporter SPANS = InMemorySpanExporter.create();

    @Produces
    @Singleton
    SpanProcessor spans() {
        return SimpleSpanProcessor.create(SPANS);
    }

    @Produces
    @Singleton
    MeterRegistry meters() {
        return new SimpleMeterRegistry();
    }
}
