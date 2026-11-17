package io.quarkiverse.modulith.observability.runtime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.spi.TransactionRunner;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Registers a gauge per publication status over the event publication repository, a Quarkus extra, when an event
 * publication store is present. A background thread counts the publications at a fixed interval, so metrics scrapes
 * never query the store.
 */
@Singleton
public class PublicationGauges {

    static final String PREFIX = "quarkus.modulith.observability.metrics.publications.";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("modulith-publication-gauges").daemon().factory());

    @Inject
    MeterRegistry registry;

    @Inject
    EventPublicationRepository repository;

    @Inject
    TransactionRunner transactions;

    @Inject
    PublicationGaugesConfig config;

    void start(@Observes StartupEvent event) {
        if (!config.enabled()) {
            return;
        }
        if (config.refreshInterval().isNegative() || config.refreshInterval().isZero()) {
            throw new ConfigurationException(PREFIX + "refresh-interval must be positive, but is "
                    + config.refreshInterval() + ". Set " + PREFIX + "enabled=false to turn the gauges off");
        }
        PublicationCounts counts = new PublicationCounts(repository, transactions, config.includeCompleted());
        counts.register(registry);
        scheduler.scheduleWithFixedDelay(counts::refresh, 0, config.refreshInterval().toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }
}
