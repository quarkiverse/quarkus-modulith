package io.quarkiverse.modulith.events;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

/**
 * Access to completed publications, to inspect and clean them up.
 */
public interface CompletedEventPublications {

    List<EventPublication> findAll();

    /**
     * @return the number of deleted publications
     */
    int deletePublications(Predicate<EventPublication> filter);

    /**
     * Deletes the publications completed before {@code now - duration}.
     *
     * @return the number of deleted publications
     */
    int deletePublicationsOlderThan(Duration duration);
}
