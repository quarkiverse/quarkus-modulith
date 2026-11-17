package io.quarkiverse.modulith.it.eventsjdbc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseSchemaInitializer;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseType;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcOperations;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcRepositorySettings;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcStore;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

class JdbcFailureTest {

    private static final TckEventSerializer SERIALIZER = new TckEventSerializer();

    @Test
    void reportsAnUnreachableDatabase() {
        // act
        Throwable thrown = catchThrowable(() -> JdbcStore.create(Databases.unreachable(), SERIALIZER, Optional.empty(),
                false, CompletionMode.UPDATE));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot detect the database of the event publication registry");
    }

    @Test
    void reportsFailingSchemaInitialization() {
        // arrange
        JdbcStore store = new JdbcStore(new JdbcOperations(Databases.unreachable()), SERIALIZER,
                new JdbcRepositorySettings(DatabaseType.H2, Optional.empty(), false, CompletionMode.UPDATE));

        // act
        Throwable thrown = catchThrowable(() -> new DatabaseSchemaInitializer(store).initialize());

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create the event publication tables");
    }

    @Test
    void reportsFailingQueries() {
        // arrange
        EventPublicationRepository repository = repositoryOnMissingTables();

        // act
        Throwable thrown = catchThrowable(repository::findCompletedPublications);

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Event publication statement failed: SELECT ID, COMPLETION_DATE, EVENT_TYPE, LISTENER_ID, "
                        + "PUBLICATION_DATE, SERIALIZED_EVENT, STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE "
                        + "FROM missing.EVENT_PUBLICATION WHERE COMPLETION_DATE IS NOT NULL ORDER BY PUBLICATION_DATE ASC");
    }

    @Test
    void reportsFailingUpdates() {
        // arrange
        EventPublicationRepository repository = repositoryOnMissingTables();

        // act
        Throwable thrown = catchThrowable(
                () -> repository.markFailed(UUID.fromString("00000000-0000-0000-0000-000000000001")));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Event publication statement failed: UPDATE missing.EVENT_PUBLICATION SET STATUS = 'FAILED' "
                        + "WHERE ID = ? AND COMPLETION_DATE IS NULL");
    }

    private static EventPublicationRepository repositoryOnMissingTables() {
        return new JdbcEventPublicationRepository(new JdbcStore(
                new JdbcOperations(Databases.dataSource(TestDatabase.H2)), SERIALIZER,
                new JdbcRepositorySettings(DatabaseType.H2, Optional.of("missing"), false, CompletionMode.UPDATE)));
    }
}
