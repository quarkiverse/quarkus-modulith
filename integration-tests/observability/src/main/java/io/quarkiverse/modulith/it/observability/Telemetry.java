package io.quarkiverse.modulith.it.observability;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

/**
 * Collects the finished spans in memory and serves them, so the tests read them over HTTP in JVM and native mode.
 */
@Path("/telemetry/spans")
public class Telemetry {

    static final InMemorySpanExporter SPANS = InMemorySpanExporter.create();

    @Produces
    @Singleton
    SpanProcessor spans() {
        return SimpleSpanProcessor.create(SPANS);
    }

    @GET
    public List<Map<String, Object>> list() {
        return SPANS.getFinishedSpanItems().stream()
                .map(it -> Map.<String, Object> of("name", it.getName(), "attributes", it.getAttributes().asMap()
                        .entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getKey(),
                                e -> String.valueOf(e.getValue())))))
                .toList();
    }

    @DELETE
    public void clear() {
        SPANS.reset();
    }
}
