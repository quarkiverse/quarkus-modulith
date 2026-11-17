package io.quarkiverse.modulith.events.core;

/**
 * Declares listeners whose publications a component other than the registry delivers, so that the registry's
 * resubmissions (at startup, periodically and from {@code IncompleteEventPublications} and
 * {@code FailedEventPublications}) leave them alone. Event externalization in outbox mode declares its listener, so
 * only its drain sends those publications, in order.
 * <p>
 * Implementations are CDI beans. This is a service provider interface for Quarkus Modulith extensions.
 */
public interface ResubmissionExclusion {

    /**
     * @return whether the registry must not resubmit publications of this listener
     */
    boolean excludes(PublicationTargetIdentifier listener);
}
