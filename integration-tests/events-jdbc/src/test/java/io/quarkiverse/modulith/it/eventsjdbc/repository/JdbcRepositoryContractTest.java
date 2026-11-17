package io.quarkiverse.modulith.it.eventsjdbc.repository;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseSchemaInitializer;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcStore;
import io.quarkiverse.modulith.events.jdbc.runtime.LegacyJdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.tck.EventPublicationRepositoryContract;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

@ParameterizedClass(name = "{0}")
@MethodSource("targets")
class JdbcRepositoryContractTest extends EventPublicationRepositoryContract {

    private final RepositoryTarget target;
    private final EventPublicationRepository repository;

    JdbcRepositoryContractTest(RepositoryTarget target) {
        this.target = target;
        this.repository = repository(target);
    }

    static Stream<RepositoryTarget> targets() {
        return TestDatabase.selected(Arrays.stream(TestDatabase.values()))
                .flatMap(database -> Stream.of(false, true)
                        .flatMap(legacy -> Stream.of(CompletionMode.UPDATE, CompletionMode.ARCHIVE)
                                .map(completion -> new RepositoryTarget(database, legacy, completion))));
    }

    @BeforeParameterizedClassInvocation
    static void createTables(RepositoryTarget target) {
        Tables.drop(Databases.dataSource(target.database()), Optional.empty());
        new DatabaseSchemaInitializer(store(target)).initialize();
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
    protected boolean tracksStatus() {
        return !target.legacy();
    }

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }

    static JdbcStore store(RepositoryTarget target) {
        DataSource dataSource = Databases.dataSource(target.database());
        return JdbcStore.create(dataSource, new TckEventSerializer(), Optional.empty(), target.legacy(),
                target.completion());
    }

    static EventPublicationRepository repository(RepositoryTarget target) {
        JdbcStore store = store(target);
        return target.legacy() ? new LegacyJdbcEventPublicationRepository(store)
                : new JdbcEventPublicationRepository(store);
    }

}
