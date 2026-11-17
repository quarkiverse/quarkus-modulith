package io.quarkiverse.modulith.events.registry;

import java.util.List;
import java.util.function.Consumer;

import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * A store that commits its own work after the JTA transaction, like the MongoDB store, and delivers after that.
 */
final class OwnCommitRepository extends InMemoryEventPublicationRepository {

    private final List<String> order;

    OwnCommitRepository(List<String> order) {
        this.order = order;
    }

    @Override
    public void afterCommit(Runnable action, Consumer<Runnable> afterTransactionCommit) {
        afterTransactionCommit.accept(() -> {
            order.add("store-committed");
            action.run();
        });
    }
}
