package io.quarkiverse.modulith.it.eventsjdbc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseSchemaInitializer;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcStore;
import io.quarkiverse.modulith.events.jdbc.runtime.LegacyJdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

@ParameterizedClass(name = "{0}")
@MethodSource("databases")
class JdbcRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");
    private static final TckEventSerializer SERIALIZER = new TckEventSerializer();

    private final TestDatabase database;

    JdbcRepositoryTest(TestDatabase database) {
        this.database = database;
    }

    /**
     * Oracle before SQL Server, so the heavy container the contract test left running is reused.
     */
    static Stream<TestDatabase> databases() {
        return TestDatabase.selected(Stream.of(TestDatabase.H2, TestDatabase.HSQLDB, TestDatabase.POSTGRESQL,
                TestDatabase.MYSQL, TestDatabase.MARIADB, TestDatabase.ORACLE, TestDatabase.SQLSERVER));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deleteCompletionRemovesTheRow(boolean legacy) {
        // arrange
        EventPublicationRepository repository = repository(legacy, CompletionMode.DELETE);
        TargetEventPublication completed = repository.create(publication("completed", 0));
        repository.create(publication("incomplete", 1));

        // act
        repository.markCompleted(completed.getIdentifier(), T0.plusSeconds(5));

        // assert
        assertThat(events(repository.findIncompletePublications())).containsExactly("incomplete");
        assertThat(repository.findCompletedPublications()).isEmpty();
        assertThat(repository.countByStatus(Status.COMPLETED)).isZero();
    }

    @Test
    void createStoresEveryColumn() {
        // arrange
        EventPublicationRepository repository = repository(false, CompletionMode.UPDATE);
        UUID id = UUID.randomUUID();

        // act
        repository.create(TargetEventPublication.of(id, new TckEvent("e-1"), TckEvent.class.getName(), LISTENER, T0,
                Optional.empty(), Status.RESUBMITTED, 3, Optional.of(T0.plusSeconds(7))));

        // assert
        assertThat(single(repository.findIncompletePublications())).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(id);
            assertThat(p.getStatus()).isEqualTo(Status.RESUBMITTED);
            assertThat(p.getCompletionAttempts()).isEqualTo(3);
            assertThat(p.getLastResubmissionDate()).hasValue(T0.plusSeconds(7));
            assertThat(p.getCompletionDate()).isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void createStoresACompletionDate(boolean legacy) {
        // arrange
        EventPublicationRepository repository = repository(legacy, CompletionMode.UPDATE);

        // act
        repository.create(TargetEventPublication.of(UUID.randomUUID(), new TckEvent("done"), TckEvent.class.getName(),
                LISTENER, T0, Optional.of(T0.plusSeconds(3)), Status.COMPLETED, 1, Optional.empty()));

        // assert
        assertThat(single(repository.findCompletedPublications())).satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(3));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void readsPublicationsOfUnknownEventTypesUntilTheirEventIsUsed(boolean legacy) {
        // arrange
        EventPublicationRepository repository = repository(legacy, CompletionMode.UPDATE);
        repository.create(TargetEventPublication.of(UUID.randomUUID(), new TckEvent("gone"), "com.acme.Removed",
                LISTENER, T0, Optional.empty(), Status.PUBLISHED, 0, Optional.empty()));
        repository.create(publication("known", 1));

        // act
        List<TargetEventPublication> incomplete = repository.findIncompletePublications();
        Throwable thrown = catchThrowable(() -> incomplete.get(0).getEvent());

        // assert
        assertThat(incomplete).extracting(TargetEventPublication::getEventType)
                .containsExactly("com.acme.Removed", TckEvent.class.getName());
        assertThat(incomplete.get(1).getEvent()).isEqualTo(new TckEvent("known"));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Cannot read event publication ").hasMessageEndingWith(" of type com.acme.Removed");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void pagesWithoutAnUpperBound(boolean legacy) {
        // arrange
        EventPublicationRepository repository = repository(legacy, CompletionMode.UPDATE);
        repository.create(publication("first", 0));
        repository.create(publication("second", 1));

        // act
        List<TargetEventPublication> page = repository.findIncompletePublications(PageCriteria.first(Instant.MAX, 10));

        // assert
        assertThat(events(page)).containsExactly("first", "second");
    }

    @Test
    void claimsNothingWithoutEligibleStatuses() {
        // arrange
        EventPublicationRepository repository = repository(false, CompletionMode.UPDATE);
        TargetEventPublication open = repository.create(publication("open", 0));

        // act
        boolean claimed = repository.claim(open.getIdentifier(), Set.of(), T0, T0.plusSeconds(5));

        // assert
        assertThat(claimed).isFalse();
        assertThat(single(repository.findIncompletePublications()).getStatus()).isEqualTo(Status.PUBLISHED);
    }

    @Test
    void legacyStructureClaimsAndPagesIncompletePublicationsAsPublished() {
        // arrange
        EventPublicationRepository repository = repository(true, CompletionMode.UPDATE);
        TargetEventPublication open = repository.create(publication("open", 0));
        repository.create(TargetEventPublication.of(new TckEvent("other"), PublicationTargetIdentifier.of("mail.on-order"),
                T0.plusSeconds(1)));
        PageCriteria page = PageCriteria.first(T0.plusSeconds(10), 10);

        // act
        boolean claimed = repository.claim(open.getIdentifier(), Set.of(Status.PUBLISHED), T0, T0.plusSeconds(5));
        boolean failedOnly = repository.claim(open.getIdentifier(), Set.of(Status.FAILED), T0, T0.plusSeconds(5));
        boolean unknown = repository.claim(UUID.randomUUID(), Set.of(Status.PUBLISHED), T0, T0.plusSeconds(5));
        List<TargetEventPublication> published = repository.findIncompletePublications(LISTENER,
                Set.of(Status.PUBLISHED), page);
        List<TargetEventPublication> failed = repository.findIncompletePublications(LISTENER, Set.of(Status.FAILED),
                page);
        int stale = repository.markStalePublicationsFailed(Status.PUBLISHED, T0.plusSeconds(10));

        // assert
        assertThat(claimed).isTrue();
        assertThat(failedOnly).isFalse();
        assertThat(unknown).isFalse();
        assertThat(events(published)).containsExactly("open");
        assertThat(failed).isEmpty();
        assertThat(stale).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deletesManyPublicationsInBatches(boolean legacy) {
        // arrange
        EventPublicationRepository repository = repository(legacy, CompletionMode.ARCHIVE);
        List<UUID> ids = IntStream.range(0, 205)
                .mapToObj(i -> repository.create(publication("e-" + i, i)).getIdentifier())
                .toList();
        repository.create(publication("kept", 300));

        // act
        repository.deletePublications(ids);

        // assert
        assertThat(events(repository.findIncompletePublications())).containsExactly("kept");
    }

    @ParameterizedTest
    @CsvSource({ "false, UPDATE", "true, UPDATE", "false, ARCHIVE", "true, ARCHIVE" })
    void keepsTheFirstCompletionOfAPublicationCompletedTwice(boolean legacy, CompletionMode completion) {
        // arrange
        EventPublicationRepository repository = repository(legacy, completion);
        TargetEventPublication publication = repository.create(publication("e-1", 0));
        repository.markCompleted(publication.getIdentifier(), T0.plusSeconds(5));

        // act
        repository.markCompleted(publication.getIdentifier(), T0.plusSeconds(9));

        // assert
        assertThat(single(repository.findCompletedPublications()).getCompletionDate()).hasValue(T0.plusSeconds(5));
    }

    @Test
    void legacyStructureTracksCompletionOnly() {
        // arrange
        EventPublicationRepository repository = repository(true, CompletionMode.UPDATE);
        TargetEventPublication open = repository.create(publication("open", 0));
        TargetEventPublication done = repository.create(publication("done", 1));
        repository.markCompleted(done.getIdentifier(), T0.plusSeconds(2));

        // act
        repository.markFailed(open.getIdentifier());
        boolean processingOpen = repository.markProcessing(open.getIdentifier());
        boolean processingDone = repository.markProcessing(done.getIdentifier());
        boolean resubmittedOpen = repository.markResubmitted(open.getIdentifier(), T0.plusSeconds(3));
        boolean resubmittedUnknown = repository.markResubmitted(UUID.randomUUID(), T0.plusSeconds(3));

        // assert
        assertThat(processingOpen).isTrue();
        assertThat(processingDone).isFalse();
        assertThat(resubmittedOpen).isTrue();
        assertThat(resubmittedUnknown).isFalse();
        assertThat(single(repository.findIncompletePublications())).satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.PUBLISHED);
            assertThat(p.getCompletionAttempts()).isZero();
            assertThat(p.getLastResubmissionDate()).isEmpty();
        });
        assertThat(repository.findFailedPublications(FailedCriteria.all())).isEmpty();
        assertThat(events(repository.findByStatus(Status.PUBLISHED))).containsExactly("open");
        assertThat(events(repository.findByStatus(Status.COMPLETED))).containsExactly("done");
        assertThat(repository.findByStatus(Status.FAILED)).isEmpty();
        assertThat(repository.countByStatus(Status.PUBLISHED)).isEqualTo(1);
        assertThat(repository.countByStatus(Status.COMPLETED)).isEqualTo(1);
        assertThat(repository.countByStatus(Status.PROCESSING)).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void schemaInitializationBacksOffWhenTheTableExists(boolean legacy) {
        // arrange
        DataSource dataSource = Databases.dataSource(database);
        Tables.drop(dataSource, Optional.empty());
        Tables.execute(dataSource, "CREATE TABLE EVENT_PUBLICATION (ID VARCHAR(36) NOT NULL)");
        JdbcStore store = store(legacy, CompletionMode.ARCHIVE, Optional.empty());

        // act
        new DatabaseSchemaInitializer(store).initialize();
        new DatabaseSchemaInitializer(store).initialize();

        // assert
        assertThat(Tables.columns(dataSource, Optional.empty(), "EVENT_PUBLICATION")).containsExactly("ID");
        assertThat(Tables.columns(dataSource, Optional.empty(), "EVENT_PUBLICATION_ARCHIVE")).hasSize(legacy ? 6 : 9);
    }

    @Test
    void usesTheConfiguredSchema() {
        // arrange
        assumeTrue(database.type().isSchemaSupported(), "Only H2, HSQLDB and PostgreSQL support a schema name");
        DataSource dataSource = Databases.dataSource(database);
        Optional<String> schema = Optional.of("events");
        Tables.drop(dataSource, schema);
        Tables.drop(dataSource, Optional.empty());
        JdbcStore store = store(false, CompletionMode.ARCHIVE, schema);
        new DatabaseSchemaInitializer(store).initialize();
        EventPublicationRepository repository = new JdbcEventPublicationRepository(store);
        TargetEventPublication publication = repository.create(publication("e-1", 0));

        // act
        repository.markCompleted(publication.getIdentifier(), T0.plusSeconds(1));

        // assert
        assertThat(events(repository.findCompletedPublications())).containsExactly("e-1");
        assertThat(Tables.exists(dataSource, schema, "EVENT_PUBLICATION_ARCHIVE")).isTrue();
        assertThat(Tables.exists(dataSource, Optional.empty(), "EVENT_PUBLICATION")).isFalse();
    }

    @Test
    void detectsTheDatabase() {
        // act
        JdbcStore store = JdbcStore.create(Databases.dataSource(database), SERIALIZER, Optional.empty(), false,
                CompletionMode.UPDATE);

        // assert
        assertThat(store.settings().databaseType()).isEqualTo(database.type());
    }

    private EventPublicationRepository repository(boolean legacy, CompletionMode completion) {
        Tables.drop(Databases.dataSource(database), Optional.empty());
        JdbcStore store = store(legacy, completion, Optional.empty());
        new DatabaseSchemaInitializer(store).initialize();
        return legacy ? new LegacyJdbcEventPublicationRepository(store) : new JdbcEventPublicationRepository(store);
    }

    private JdbcStore store(boolean legacy, CompletionMode completion, Optional<String> schema) {
        return JdbcStore.create(Databases.dataSource(database), SERIALIZER, schema, legacy, completion);
    }

    private static TargetEventPublication publication(String event, int secondsAfterT0) {
        return TargetEventPublication.of(new TckEvent(event), LISTENER, T0.plusSeconds(secondsAfterT0));
    }

    private static TargetEventPublication single(List<TargetEventPublication> publications) {
        assertThat(publications).hasSize(1);
        return publications.get(0);
    }

    private static List<String> events(List<TargetEventPublication> publications) {
        return publications.stream().map(p -> ((TckEvent) p.getEvent()).id()).toList();
    }
}
