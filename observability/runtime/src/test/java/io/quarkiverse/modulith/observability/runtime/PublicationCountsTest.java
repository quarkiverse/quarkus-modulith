package io.quarkiverse.modulith.observability.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

class PublicationCountsTest {

    static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @ParameterizedTest
    @CsvSource({ "false, 'failed, processing, published, resubmitted'",
            "true, 'completed, failed, processing, published, resubmitted'" })
    void countsCompletedPublicationsOnlyWhenAsked(boolean includeCompleted, String statuses) {
        // arrange
        MeterRegistry registry = new SimpleMeterRegistry();
        PublicationCounts counts = new PublicationCounts(new FlakyRepository(), new CountingTransactions(),
                includeCompleted);

        // act
        counts.register(registry);

        // assert
        assertThat(registry.find("modulith.event.publications").gauges())
                .extracting(it -> it.getId().getTag("status"))
                .containsExactlyInAnyOrder(statuses.split(", "));
    }

    @Test
    void readsTheLastCountInsteadOfQueryingTheStore() {
        // arrange
        MeterRegistry registry = new SimpleMeterRegistry();
        FlakyRepository repository = new FlakyRepository();
        CountingTransactions transactions = new CountingTransactions();
        PublicationCounts counts = new PublicationCounts(repository, transactions, false);
        counts.register(registry);
        Gauge published = registry.get("modulith.event.publications").tag("status", "published").gauge();
        double before = published.value();
        repository.create(TargetEventPublication.of("event", PublicationTargetIdentifier.of("listener"), NOW));
        double unrefreshed = published.value();

        // act
        counts.refresh();

        // assert
        assertThat(before).isNaN();
        assertThat(unrefreshed).isNaN();
        assertThat(published.value()).isEqualTo(1);
        assertThat(registry.get("modulith.event.publications").tag("status", "failed").gauge().value()).isZero();
        assertThat(transactions.started).isEqualTo(1);
    }

    @Test
    void keepsThePreviousCountsWhenTheStoreFails() {
        // arrange
        MeterRegistry registry = new SimpleMeterRegistry();
        FlakyRepository repository = new FlakyRepository();
        repository.create(TargetEventPublication.of("event", PublicationTargetIdentifier.of("listener"), NOW));
        PublicationCounts counts = new PublicationCounts(repository, new CountingTransactions(), false);
        counts.register(registry);
        counts.refresh();
        repository.create(TargetEventPublication.of("other", PublicationTargetIdentifier.of("listener"), NOW));
        repository.down = true;

        // act
        counts.refresh();

        // assert
        assertThat(registry.get("modulith.event.publications").tag("status", "published").gauge().value())
                .isEqualTo(1);
    }

    @Test
    void countsAgainWhenTheStoreIsBack() {
        // arrange
        MeterRegistry registry = new SimpleMeterRegistry();
        FlakyRepository repository = new FlakyRepository();
        PublicationCounts counts = new PublicationCounts(repository, new CountingTransactions(), false);
        counts.register(registry);
        repository.down = true;
        counts.refresh();
        counts.refresh();
        repository.create(TargetEventPublication.of("event", PublicationTargetIdentifier.of("listener"), NOW));
        repository.down = false;

        // act
        counts.refresh();
        counts.refresh();

        // assert
        assertThat(registry.get("modulith.event.publications").tag("status", "published").gauge().value())
                .isEqualTo(1);
    }
}
