package io.quarkiverse.modulith.observability.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.runtime.configuration.ConfigurationException;

class PublicationGaugesTest {

    @ParameterizedTest
    @ValueSource(strings = { "PT0S", "PT-1S" })
    void rejectsARefreshIntervalThatIsNotPositive(String interval) {
        // arrange
        PublicationGauges gauges = gauges(new GaugeSettings(true, Duration.parse(interval), false));

        // act
        Throwable thrown = catchThrowable(() -> gauges.start(null));

        // assert
        assertThat(thrown).isExactlyInstanceOf(ConfigurationException.class).hasMessage(
                "quarkus.modulith.observability.metrics.publications.refresh-interval must be positive, but is "
                        + Duration.parse(interval) + ". Set quarkus.modulith.observability.metrics.publications"
                        + ".enabled=false to turn the gauges off");
        assertThat(gauges.registry.getMeters()).isEmpty();
    }

    @Test
    void registersNoGaugeWhenDisabled() {
        // arrange
        PublicationGauges gauges = gauges(new GaugeSettings(false, Duration.ZERO, false));

        // act
        gauges.start(null);

        // assert
        assertThat(gauges.registry.getMeters()).isEmpty();
    }

    static PublicationGauges gauges(PublicationGaugesConfig config) {
        PublicationGauges gauges = new PublicationGauges();
        gauges.registry = new SimpleMeterRegistry();
        gauges.repository = new FlakyRepository();
        gauges.transactions = new CountingTransactions();
        gauges.config = config;
        return gauges;
    }
}
