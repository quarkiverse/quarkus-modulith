package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.modulith.observability.deployment.app.ObservedApplication;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderService;
import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

public class MetricsOnlyTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ObservedApplication.class.getPackage())
                    .addClasses(TestTelemetry.class, TestRepository.class))
            .addBuildChainCustomizer(MetricsOnlyTest::provideStoreCapability)
            .overrideConfigKey("quarkus.modulith.observability.metrics.publications.refresh-interval", "PT0.05S")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.observability.tracing.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    MeterRegistry meters;

    @Test
    void countsWithoutModuleSpans() {
        // arrange
        TestTelemetry.SPANS.reset();

        // act
        orders.complete("1", List.of(), new int[0]);

        // assert
        assertThat(TestTelemetry.SPANS.getFinishedSpanItems()).extracting(it -> it.getName()).containsExactly("inner");
        assertThat(meters.get("module.events.published").counter().count()).isEqualTo(1);
        assertThat(meters.get("module.invocations").tags("module.identifier", "order").timer().count()).isEqualTo(1);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(
                meters.get("modulith.event.publications").tags("status", "published").gauge().value()).isEqualTo(1));
        assertThat(meters.find("modulith.event.publications").gauges())
                .extracting(it -> it.getId().getTag("status"))
                .containsExactlyInAnyOrder("published", "processing", "failed", "resubmitted");
    }

    /**
     * Stands in for an event publication store extension, which provides the store capability, so that the test
     * application with {@link TestRepository} gets the publication gauges.
     */
    static void provideStoreCapability(BuildChainBuilder chain) {
        chain.addBuildStep(context -> context.produce(
                new CapabilityBuildItem(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY, "test-store")))
                .produces(CapabilityBuildItem.class)
                .build();
    }
}
