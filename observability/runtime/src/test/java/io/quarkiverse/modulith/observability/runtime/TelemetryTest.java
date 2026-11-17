package io.quarkiverse.modulith.observability.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;

class TelemetryTest {

    @Test
    void passesTheModuleToNestedSpansWithoutOverriding() {
        // arrange
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        ModuleIdentifierSpanProcessor processor = new ModuleIdentifierSpanProcessor();
        Tracer tracer = SdkTracerProvider.builder().addSpanProcessor(processor)
                .addSpanProcessor(SimpleSpanProcessor.create(exporter)).build().get("test");
        Context module = Context.root().with(ModuleContextKey.KEY, "order");

        // act
        tracer.spanBuilder("nested").setParent(module).startSpan().end();
        tracer.spanBuilder("tagged").setParent(module).setAttribute("module.identifier", "own").startSpan().end();
        tracer.spanBuilder("outside").setParent(Context.root()).startSpan().end();

        // assert
        assertThat(exporter.getFinishedSpanItems()).extracting(it -> it.getName(),
                it -> it.getAttributes().get(AttributeKey.stringKey("module.identifier")))
                .containsExactly(tuple("nested", "order"),
                        tuple("tagged", "own"),
                        tuple("outside", null));
    }

    @Test
    void onlyNeedsTheStartOfSpans() {
        // arrange
        ModuleIdentifierSpanProcessor processor = new ModuleIdentifierSpanProcessor();

        // act
        boolean start = processor.isStartRequired();
        boolean end = processor.isEndRequired();
        Throwable ending = catchThrowable(() -> processor.onEnd(null));

        // assert
        assertThat(start).isTrue();
        assertThat(end).isFalse();
        assertThat(ending).isNull();
    }

    @Test
    void ignoresEventsOutsideModules() {
        // arrange
        ModuleEventMetrics metrics = new ModuleEventMetrics();
        metrics.registry = new SimpleMeterRegistry();
        metrics.modules = new ApplicationModules(new ModelData("Shop", "com.acme", List.of(), List.of(), ""));
        metrics.factory = new ModuleEventCounterFactory(List.of());

        // act
        metrics.record("not a module event");

        // assert
        assertThat(metrics.registry.getMeters()).isEmpty();
    }

    @Test
    void countsTheNextEventsWhenACustomizerFails() {
        // arrange
        ModuleEventMetrics metrics = new ModuleEventMetrics();
        metrics.registry = new SimpleMeterRegistry();
        metrics.modules = new ApplicationModules(new ModelData("Shop", "io.quarkiverse.modulith.observability",
                List.of(new ModelData.ModuleData("runtime", "Runtime", "io.quarkiverse.modulith.observability.runtime",
                        false, false, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of())),
                List.of(), ""));
        metrics.factory = new ModuleEventCounterFactory(List.of(it -> it.customize(Nested.class,
                (Nested event, Counter.Builder builder) -> {
                    throw new IllegalStateException("broken customizer");
                })));

        // act
        metrics.record(new Nested());
        metrics.record(new Nested());
        metrics.record(new Target());

        // assert
        assertThat(metrics.registry.find("module.events.published").counters())
                .extracting(it -> it.getId().getTag("event_type"), Counter::count)
                .containsExactly(tuple("runtime.Target", 1.0));
    }

    @ParameterizedTest
    @CsvSource({ "java.lang.Object, java.lang.String, 1", "java.lang.String, java.lang.Object, -1",
            "java.lang.CharSequence, java.lang.String, 1", "java.lang.Integer, java.lang.String, -1",
            "java.lang.String, java.lang.Integer, 1", "java.lang.Number, java.lang.String, -1",
            "java.lang.String, java.lang.String, 0" })
    void ordersTypesMostSpecificFirst(Class<?> first, Class<?> second, int expected) {
        // act
        int order = TypeSpecificity.compare(first, second);

        // assert
        assertThat(Integer.signum(order)).isEqualTo(expected);
    }

    @Test
    void sortsSubtypesBeforeTheirSupertypesAndTheRestByDepthAndName() {
        // arrange
        List<Class<?>> types = new ArrayList<>(List.of(Object.class, CharSequence.class, Serializable.class,
                String.class, Number.class, Integer.class));

        // act
        types.sort(TypeSpecificity::compare);

        // assert
        assertThat(types).containsExactly(Integer.class, Number.class, String.class, Serializable.class,
                CharSequence.class, Object.class);
    }
}
