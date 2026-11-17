package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.modulith.observability.deployment.app.ObservedApplication;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderService;
import io.quarkiverse.modulith.observability.runtime.ObservedModules;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class ObservabilityDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ObservedApplication.class.getPackage())
                    .addClasses(TestTelemetry.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.observability.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    MeterRegistry meters;

    @Test
    void observesNothing() {
        // arrange
        TestTelemetry.SPANS.reset();

        // act
        orders.complete("1", List.of(), new int[0]);

        // assert
        assertThat(TestTelemetry.SPANS.getFinishedSpanItems()).extracting(it -> it.getName()).containsExactly("inner");
        assertThat(meters.find("module.events.published").counters()).isEmpty();
        assertThat(meters.find("module.invocations").timers()).isEmpty();
        assertThat(Arc.container().instance(ObservedModules.class).isAvailable()).isFalse();
    }
}
