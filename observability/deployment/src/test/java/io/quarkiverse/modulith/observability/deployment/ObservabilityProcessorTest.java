package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.deployment.ModuleModelBuildItem;
import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.PaymentEvent;
import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.PaymentReceived;
import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.PaymentRefunded;
import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

/**
 * Calls the build steps directly for the combinations of configuration and extensions the application tests cannot
 * produce: libraries absent, both sub-features off, a publication store or none.
 */
class ObservabilityProcessorTest {

    static final Capabilities OTEL = new Capabilities(Set.of(Capability.OPENTELEMETRY_TRACER));
    static final Capabilities STORE = new Capabilities(Set.of(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY));
    static final Optional<MetricsCapabilityBuildItem> MICROMETER = Optional
            .of(new MetricsCapabilityBuildItem(MetricsFactory.MICROMETER::equals));

    @ParameterizedTest
    @CsvSource({ "true, true, true", "false, true, false", "true, false, false" })
    void tracesWhenEnabledAndPresent(boolean enabled, boolean present, boolean expected) {
        // arrange
        Capabilities capabilities = present ? OTEL : new Capabilities(Set.of());

        // act
        boolean tracing = Observability.tracing(ObservationSettings.of(enabled, true), capabilities);

        // assert
        assertThat(tracing).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "true, micrometer, true", "false, micrometer, false", "true, other, false", "true, none, false" })
    void measuresWhenEnabledAndPresent(boolean enabled, String factory, boolean expected) {
        // arrange
        Optional<MetricsCapabilityBuildItem> capability = factory.equals("none") ? Optional.empty()
                : Optional.of(new MetricsCapabilityBuildItem(factory::equals));

        // act
        boolean metrics = Observability.metrics(ObservationSettings.of(true, enabled), capability);

        // assert
        assertThat(metrics).isEqualTo(expected);
    }

    @Test
    void observesNothingWithoutTracingAndMetrics() {
        // arrange
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();

        // act
        Optional<ObservedTypesBuildItem> types = new ObservabilityProcessor().observedTypes(
                ObservationSettings.of(false, false), OTEL, MICROMETER, null, null, beans::add);

        // assert
        assertThat(types).isEmpty();
        assertThat(beans).isEmpty();
    }

    @Test
    void countsNothingWithoutMetrics() {
        // arrange
        List<Object> produced = new ArrayList<>();
        ObservabilityProcessor processor = new ObservabilityProcessor();

        // act
        processor.eventBeans(ObservationSettings.of(true, false), MICROMETER, STORE, produced::add, produced::add);
        processor.eventObservers(ObservationSettings.of(true, true), Optional.empty(), null, null, null, produced::add);

        // assert
        assertThat(produced).isEmpty();
    }

    @Test
    void registersNoGaugesWithoutAPublicationStore() {
        // arrange
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();
        Capabilities capabilities = new Capabilities(Set.of(Capability.TRANSACTIONS));

        // act
        new ObservabilityProcessor().eventBeans(ObservationSettings.of(true, true), MICROMETER, capabilities,
                beans::add, item -> {
                });

        // assert
        assertThat(beans).flatExtracting(AdditionalBeanBuildItem::getBeanClasses).containsExactly(
                "io.quarkiverse.modulith.observability.runtime.ModuleEventMetrics",
                "io.quarkiverse.modulith.observability.runtime.ModuleEventCounterFactory");
    }

    @Test
    void countsPublicationsWhenAStoreIsPresent() {
        // arrange
        List<AdditionalBeanBuildItem> beans = new ArrayList<>();

        // act
        new ObservabilityProcessor().eventBeans(ObservationSettings.of(true, true), MICROMETER, STORE, beans::add,
                item -> {
                });

        // assert
        assertThat(beans).flatExtracting(AdditionalBeanBuildItem::getBeanClasses).containsExactly(
                "io.quarkiverse.modulith.observability.runtime.ModuleEventMetrics",
                "io.quarkiverse.modulith.observability.runtime.ModuleEventCounterFactory",
                "io.quarkiverse.modulith.observability.runtime.PublicationGauges");
        assertThat(beans.get(1).isRemovable()).isTrue();
    }

    @Test
    void observesNoTypeWhoseSupertypeIsObserved() throws IOException {
        // arrange
        Index index = Index.of(PaymentEvent.class, PaymentReceived.class, PaymentRefunded.class);
        Set<String> types = Set.of(PaymentEvent.class.getName(), PaymentReceived.class.getName(),
                PaymentRefunded.class.getName(), String.class.getName());

        // act
        Set<String> observed = ObservabilityProcessor.withoutSubtypes(types, index);

        // assert
        assertThat(observed).containsExactly(PaymentEvent.class.getName(), String.class.getName());
    }

    @Test
    void findsNoEventTypesWithoutModel() {
        // arrange
        ModuleModelBuildItem model = new ModuleModelBuildItem(null, "");

        // act
        Set<String> types = ObservabilityProcessor.eventTypes(model);

        // assert
        assertThat(types).isEmpty();
    }
}
