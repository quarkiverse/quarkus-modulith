package io.quarkiverse.modulith.it.eventsjdbc.repository;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseSchemaInitializer;
import io.quarkiverse.modulith.events.tck.DeleteCompletionModeContract;

@ParameterizedClass(name = "{0}")
@MethodSource("targets")
class JdbcDeleteContractTest extends DeleteCompletionModeContract {

    private final RepositoryTarget target;
    private final EventPublicationRepository repository;

    JdbcDeleteContractTest(RepositoryTarget target) {
        this.target = target;
        this.repository = JdbcRepositoryContractTest.repository(target);
    }

    static Stream<RepositoryTarget> targets() {
        return TestDatabase.selected(Arrays.stream(TestDatabase.values()))
                .flatMap(database -> Stream.of(false, true)
                        .map(legacy -> new RepositoryTarget(database, legacy, CompletionMode.DELETE)));
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
}
