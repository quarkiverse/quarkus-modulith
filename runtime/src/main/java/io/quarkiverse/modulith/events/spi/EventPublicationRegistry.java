package io.quarkiverse.modulith.events.spi;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Stores event publications so that deliveries survive failures and restarts. Implemented by
 * {@code quarkus-modulith-events} on top of a store such as {@code quarkus-modulith-events-jpa}. Without an
 * implementation, events are delivered in memory only.
 * <p>
 * The dispatcher calls the methods in this order for each delivery: {@link #store} in the publishing transaction,
 * {@link #markProcessing} in a short transaction of its own, then {@link #markCompleted} or {@link #markFailed}.
 */
public interface EventPublicationRegistry {

    /**
     * Stores one publication per listener. Runs inside the publishing transaction when there is one, so that the
     * publications are committed or rolled back together with the business data.
     *
     * @return the publication identifiers, in the order of {@code listenerIds}
     */
    List<UUID> store(Object event, List<String> listenerIds);

    /**
     * Marks a publication as being processed, right before its listener runs. Called in a transaction of its own.
     *
     * @return {@code false} if the publication is completed or gone, so the listener must not run
     */
    default boolean markProcessing(UUID publicationId) {
        return true;
    }

    /**
     * Runs the deliveries of a publishing transaction once the stored publications are visible, which depends on the
     * store. By default right after the JTA transaction commits.
     *
     * @param deliveries the deliveries
     * @param afterTransactionCommit runs an action after the JTA transaction committed, and drops it on rollback
     */
    default void afterCommit(Runnable deliveries, Consumer<Runnable> afterTransactionCommit) {
        afterTransactionCommit.accept(deliveries);
    }

    /**
     * Marks a publication as completed. Called inside the listener's transaction, or in a transaction of its own after
     * it, depending on {@code quarkus.modulith.listeners.completion-timing}.
     */
    void markCompleted(UUID publicationId);

    /**
     * Records a failed delivery. Called after the listener's transaction has been rolled back.
     */
    void markFailed(UUID publicationId, Throwable failure);
}
