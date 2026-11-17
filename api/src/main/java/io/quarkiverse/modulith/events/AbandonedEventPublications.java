package io.quarkiverse.modulith.events;

import java.util.List;
import java.util.function.Predicate;

/**
 * Access to publications that reached {@code quarkus.modulith.events.resubmission.abandon-after-attempts} completion
 * attempts without completing. Resubmissions skip them, so they need a decision: inspect, fix and delete them. Like
 * Spring Modulith's {@code AbandonedEventPublications}.
 */
public interface AbandonedEventPublications {

    /**
     * @return the abandoned publications, oldest first, empty when no attempt limit is configured
     */
    List<EventPublication> findAll();

    /**
     * Deletes the abandoned publications matching the filter.
     *
     * @return the number of deleted publications
     */
    int deletePublications(Predicate<EventPublication> filter);
}
