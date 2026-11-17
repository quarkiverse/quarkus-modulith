package io.quarkiverse.modulith.events;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Bounds a resubmission of publications, like Spring Modulith's {@code ResubmissionOptions}. Immutable: every
 * {@code with} method returns a copy.
 */
public final class ResubmissionOptions {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final int maxInFlight;
    private final int batchSize;
    private final Duration minAge;
    private final Predicate<EventPublication> filter;

    private ResubmissionOptions(int maxInFlight, int batchSize, Duration minAge, Predicate<EventPublication> filter) {
        this.maxInFlight = maxInFlight;
        this.batchSize = batchSize;
        this.minAge = minAge;
        this.filter = filter;
    }

    /**
     * @return options without limit on publications in flight, batches of 100, no minimum age and no filter
     */
    public static ResubmissionOptions defaults() {
        return new ResubmissionOptions(Integer.MAX_VALUE, DEFAULT_BATCH_SIZE, Duration.ZERO, publication -> true);
    }

    /**
     * @param maxInFlight how many resubmitted publications may be incomplete at the same time, at least 1
     */
    public ResubmissionOptions withMaxInFlight(int maxInFlight) {
        return new ResubmissionOptions(positive(maxInFlight, "maxInFlight"), batchSize, minAge, filter);
    }

    /**
     * @param batchSize how many publications are read from the store at once, at least 1
     */
    public ResubmissionOptions withBatchSize(int batchSize) {
        return new ResubmissionOptions(maxInFlight, positive(batchSize, "batchSize"), minAge, filter);
    }

    /**
     * @param minAge only publications whose last publication date is older than this are resubmitted
     */
    public ResubmissionOptions withMinAge(Duration minAge) {
        Objects.requireNonNull(minAge, "minAge");
        if (minAge.isNegative()) {
            throw new IllegalArgumentException("minAge must not be negative: " + minAge);
        }
        return new ResubmissionOptions(maxInFlight, batchSize, minAge, filter);
    }

    /**
     * @param filter only matching publications are resubmitted
     */
    public ResubmissionOptions withFilter(Predicate<EventPublication> filter) {
        return new ResubmissionOptions(maxInFlight, batchSize, minAge, Objects.requireNonNull(filter, "filter"));
    }

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Duration getMinAge() {
        return minAge;
    }

    public Predicate<EventPublication> getFilter() {
        return filter;
    }

    private static int positive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be at least 1: " + value);
        }
        return value;
    }
}
