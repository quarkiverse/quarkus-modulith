package io.quarkiverse.modulith.observability.runtime;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.spi.TransactionRunner;
import io.quarkiverse.modulith.observability.ModulithMetrics;

/**
 * The last counts of the event publications per status, which the gauges read. A gauge reads {@code NaN} until the
 * first count.
 */
final class PublicationCounts {

    private static final Logger LOG = Logger.getLogger(PublicationCounts.class);
    private static final long UNKNOWN = -1;

    private final EventPublicationRepository repository;
    private final TransactionRunner transactions;
    private final Map<Status, AtomicLong> counts = new EnumMap<>(Status.class);

    /**
     * Whether the last refresh failed. Only the refreshing thread reads and writes it.
     */
    private boolean failing;

    PublicationCounts(EventPublicationRepository repository, TransactionRunner transactions, boolean includeCompleted) {
        this.repository = repository;
        this.transactions = transactions;
        for (Status status : Status.values()) {
            if (includeCompleted || status != Status.COMPLETED) {
                counts.put(status, new AtomicLong(UNKNOWN));
            }
        }
    }

    void register(MeterRegistry registry) {
        counts.forEach((status, count) -> Gauge.builder(ModulithMetrics.PUBLICATIONS, count, PublicationCounts::value)
                .tag(ModulithMetrics.STATUS, status.name().toLowerCase(Locale.ROOT))
                .register(registry));
    }

    /**
     * Counts the publications in a new transaction. A failure keeps the previous counts, so a store that is down for a
     * moment does not stop the refreshes. The first failure is logged as a warning with its cause, the following ones
     * at debug level until a refresh works again.
     */
    void refresh() {
        try {
            transactions.callInNewTransaction(() -> {
                counts.forEach((status, count) -> count.set(repository.countByStatus(status)));
                return counts.size();
            });
            recovered();
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    private void recovered() {
        if (failing) {
            failing = false;
            LOG.info("Counting the event publications for the modulith.event.publications gauges works again");
        }
    }

    private void failed(RuntimeException e) {
        if (failing) {
            LOG.debug("Still cannot count the event publications for the modulith.event.publications gauges", e);
        } else {
            failing = true;
            LOG.warn("Cannot count the event publications for the modulith.event.publications gauges, keeping the last"
                    + " counts. Further failures are logged at debug level until counting works again", e);
        }
    }

    private static double value(AtomicLong count) {
        long value = count.get();
        return value == UNKNOWN ? Double.NaN : value;
    }
}
