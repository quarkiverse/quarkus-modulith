package io.quarkiverse.modulith.events;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

/**
 * Access to publications that have not completed yet, to submit them again.
 */
public interface IncompleteEventPublications {

    List<EventPublication> findIncompletePublications();

    /**
     * Submits the matching incomplete publications to their listeners again.
     *
     * @return the number of publications submitted
     */
    int resubmitIncompletePublications(Predicate<EventPublication> filter);

    /**
     * Submits the incomplete publications published before {@code now - duration}.
     *
     * @return the number of publications submitted
     */
    int resubmitIncompletePublicationsOlderThan(Duration duration);

    /**
     * Submits failed publications again, bounded by the options: at most {@code maxInFlight} resubmitted publications
     * at a time, one batch of at most {@code batchSize}, published before {@code now - minAge} and matching the
     * filter. Same as {@link FailedEventPublications#resubmit(ResubmissionOptions)}, as in Spring Modulith.
     *
     * @return the number of publications submitted
     */
    int resubmitIncompletePublications(ResubmissionOptions options);
}
