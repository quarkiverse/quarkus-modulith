package io.quarkiverse.modulith.it.eventsjdbc.repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseSchemaInitializer;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcOperations;
import io.quarkiverse.modulith.events.tck.MissingStatusContract;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

/**
 * Rows without status are written with plain SQL, the way a version 1 table keeps them after the migration to the
 * version 2 structure.
 */
@ParameterizedClass(name = "{0}")
@MethodSource("targets")
class JdbcMissingStatusContractTest extends MissingStatusContract {

    private static final TckEventSerializer SERIALIZER = new TckEventSerializer();

    private final RepositoryTarget target;
    private final EventPublicationRepository repository;

    JdbcMissingStatusContractTest(RepositoryTarget target) {
        this.target = target;
        this.repository = new JdbcEventPublicationRepository(JdbcRepositoryContractTest.store(target));
    }

    static Stream<RepositoryTarget> targets() {
        return TestDatabase.selected(Arrays.stream(TestDatabase.values()))
                .map(database -> new RepositoryTarget(database, false, CompletionMode.UPDATE));
    }

    @BeforeParameterizedClassInvocation
    static void createTables(RepositoryTarget target) {
        Tables.drop(Databases.dataSource(target.database()), Optional.empty());
        new DatabaseSchemaInitializer(JdbcRepositoryContractTest.store(target)).initialize();
    }

    @AfterParameterizedClassInvocation
    static void dropTables(RepositoryTarget target) {
        Tables.drop(Databases.dataSource(target.database()), Optional.empty());
    }

    @BeforeEach
    void clearTables() {
        Tables.clear(Databases.dataSource(target.database()), Optional.empty());
    }

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }

    @Override
    protected UUID insertWithoutStatus(String eventId, Optional<Instant> completionDate) {
        UUID id = UUID.randomUUID();
        new JdbcOperations(Databases.dataSource(target.database())).update("INSERT INTO EVENT_PUBLICATION (ID, "
                + "EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT, COMPLETION_DATE) VALUES ('" + id
                + "', ?, ?, ?, ?, ?)", TckEvent.class.getName(), LISTENER.value(), T0,
                SERIALIZER.serialize(new TckEvent(eventId)), completionDate);
        return id;
    }
}
