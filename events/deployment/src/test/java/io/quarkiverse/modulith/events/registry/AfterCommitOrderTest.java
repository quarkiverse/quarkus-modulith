package io.quarkiverse.modulith.events.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

class AfterCommitOrderTest {

    final List<String> order = new ArrayList<>();

    @Test
    void deliversAfterTheStoresOwnCommit() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(new OwnCommitRepository(order));

        // act
        registry.afterCommit(() -> order.add("delivered"), action -> {
            order.add("jta-committed");
            action.run();
        });

        // assert
        assertThat(order).containsExactly("jta-committed", "store-committed", "delivered");
    }

    @Test
    void deliversRightAfterTheJtaCommitByDefault() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(new InMemoryEventPublicationRepository());

        // act
        registry.afterCommit(() -> order.add("delivered"), action -> {
            order.add("jta-committed");
            action.run();
        });

        // assert
        assertThat(order).containsExactly("jta-committed", "delivered");
    }

    DefaultEventPublicationRegistry registry(EventPublicationRepository repository) {
        return new DefaultEventPublicationRegistry(repository, new DirectTransactions(), FakeEventsConfig.defaults(),
                Clock.systemUTC(), new RecordingRedelivery(Set.of()));
    }
}
