package io.quarkiverse.modulith.events.neo4j.runtime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.SessionConfig;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;

/**
 * The store has neither driver nor serializer: the guard refuses before either is used.
 */
class BlockingGuardTest {

    static Stream<Named<Consumer<EventPublicationRepository>>> operations() {
        return Stream.of(
                Named.of("markFailed", repository -> repository.markFailed(UUID.randomUUID())),
                Named.of("markProcessing", repository -> repository.markProcessing(UUID.randomUUID())),
                Named.of("findIncompletePublications", EventPublicationRepository::findIncompletePublications),
                Named.of("countByStatus", repository -> repository.countByStatus(Status.FAILED)));
    }

    @ParameterizedTest
    @MethodSource("operations")
    void refusesToBlockAnIoThread(Consumer<EventPublicationRepository> operation) {
        // arrange
        EventPublicationRepository repository = new Neo4jEventPublicationRepository(
                new Neo4jStore(null, SessionConfig.defaultConfig(), null, CompletionMode.UPDATE), () -> false);

        // act & assert
        assertThatThrownBy(() -> operation.accept(repository)).isInstanceOf(IllegalStateException.class)
                .hasMessage(Neo4jEventPublicationRepository.BLOCKING_NOT_ALLOWED);
    }
}
