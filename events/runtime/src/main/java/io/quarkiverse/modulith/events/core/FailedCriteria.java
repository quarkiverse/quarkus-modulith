package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Which failed publications {@link EventPublicationRepository#findFailedPublications} reads.
 *
 * @param maxItems at most this many publications, oldest first
 * @param publishedBefore only publications published strictly before this instant
 */
public record FailedCriteria(int maxItems, Instant publishedBefore) {

    public FailedCriteria {
        if (maxItems < 1) {
            throw new IllegalArgumentException("maxItems must be at least 1: " + maxItems);
        }
        if (Objects.isNull(publishedBefore)) {
            throw new IllegalArgumentException("publishedBefore must not be null");
        }
    }

    /**
     * @return criteria matching every failed publication, bounded by {@link EventPublicationRepository#FAR_FUTURE}
     */
    public static FailedCriteria all() {
        return new FailedCriteria(Integer.MAX_VALUE, EventPublicationRepository.FAR_FUTURE);
    }
}
