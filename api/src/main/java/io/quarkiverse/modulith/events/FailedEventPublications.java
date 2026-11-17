package io.quarkiverse.modulith.events;

/**
 * Access to publications whose listener failed, to submit only those again. Provided by event publication registries
 * as a CDI bean.
 */
public interface FailedEventPublications {

    /**
     * Submits failed publications again, bounded by the options.
     *
     * @return the number of publications submitted
     */
    int resubmit(ResubmissionOptions options);
}
