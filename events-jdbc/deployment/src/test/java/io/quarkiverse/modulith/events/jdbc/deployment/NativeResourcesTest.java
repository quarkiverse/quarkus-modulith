package io.quarkiverse.modulith.events.jdbc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseType;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

/**
 * Resource registration only matters in native builds, and extension tests cover H2 only, so the steps' helpers are
 * called directly for the other databases.
 */
class NativeResourcesTest {

    @ParameterizedTest
    @CsvSource({
            "false, false, db/modulith/events/jdbc/v2/schema-oracle.sql,",
            "false, true, db/modulith/events/jdbc/v2/schema-oracle.sql, db/modulith/events/jdbc/v2/schema-oracle-archive.sql",
            "true, false, db/modulith/events/jdbc/v1/schema-oracle.sql,",
            "true, true, db/modulith/events/jdbc/v1/schema-oracle.sql, db/modulith/events/jdbc/v1/schema-oracle-archive.sql" })
    void registersTheSchemaFilesOfTheKnownDatabaseStructureAndMode(boolean legacy, boolean archive, String table,
            String archiveTable) {
        // act
        NativeImageResourceBuildItem resources = EventsJdbcProcessor.schemaResources(Optional.of(DatabaseType.ORACLE),
                legacy, archive);

        // assert
        assertThat(resources.getResources()).containsExactlyElementsOf(
                Stream.of(table, archiveTable).filter(Objects::nonNull).toList());
    }

    @Test
    void registersTheSchemaFilesOfEveryDatabaseWhenTheDatabaseIsDetectedAtStartup() {
        // arrange
        FixedJdbcBuildTimeConfig config = new FixedJdbcBuildTimeConfig(false, Optional.of("events"));
        FixedEventsConfig events = new FixedEventsConfig(CompletionMode.ARCHIVE, Optional.empty());

        // act
        NativeImageResourceBuildItem resources = new EventsJdbcProcessor().schemas(config, events, List.of());

        // assert
        assertThat(resources.getResources()).hasSize(14).contains(
                "db/modulith/events/jdbc/v2/schema-h2.sql",
                "db/modulith/events/jdbc/v2/schema-hsqldb-archive.sql",
                "db/modulith/events/jdbc/v2/schema-sqlserver-archive.sql")
                .noneMatch(resource -> resource.contains("/v1/"));
    }

    @Test
    void registersNoArchiveTableOutsideTheArchiveMode() {
        // arrange
        FixedJdbcBuildTimeConfig config = new FixedJdbcBuildTimeConfig(false, Optional.of("events"));
        FixedEventsConfig events = new FixedEventsConfig(CompletionMode.UPDATE, Optional.empty());

        // act
        NativeImageResourceBuildItem resources = new EventsJdbcProcessor().schemas(config, events, List.of());

        // assert
        assertThat(resources.getResources()).hasSize(7).contains("db/modulith/events/jdbc/v2/schema-h2.sql")
                .noneMatch(resource -> resource.contains("-archive"));
    }

    @ParameterizedTest
    @CsvSource({
            "h2, H2",
            "postgresql, POSTGRESQL",
            "mysql, MYSQL",
            "mariadb, MARIADB",
            "mssql, SQLSERVER",
            "oracle, ORACLE" })
    void mapsTheDbKindToTheDatabase(String dbKind, DatabaseType expected) {
        // act
        Optional<DatabaseType> type = EventsJdbcProcessor.databaseType(Optional.of(dbKind));

        // assert
        assertThat(type).hasValue(expected);
        assertThat(EventsJdbcProcessor.problem(Optional.empty(), Optional.of(dbKind))).isEmpty();
    }

    @Test
    void detectsOtherDatabasesAtStartup() {
        // act
        Optional<DatabaseType> type = EventsJdbcProcessor.databaseType(Optional.of("other"));

        // assert
        assertThat(type).isEmpty();
        assertThat(EventsJdbcProcessor.problem(Optional.of("events"), Optional.of("other"))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "db2", "derby" })
    void rejectsDatabasesWithoutSchemaFiles(String dbKind) {
        // act
        Optional<String> problem = EventsJdbcProcessor.problem(Optional.empty(), Optional.of(dbKind));

        // assert
        assertThat(problem).hasValue("The JDBC event publication registry has no schema for db-kind '" + dbKind
                + "' of the default datasource. Supported: h2, mariadb, mssql, mysql, oracle, postgresql, and other "
                + "for HSQLDB.");
    }

    @Test
    void reportsAMissingDefaultDatasource() {
        // act
        Optional<String> problem = EventsJdbcProcessor.problem(Optional.empty(), Optional.empty());

        // assert
        assertThat(problem).hasValue("The JDBC event publication registry uses the default datasource, which is not "
                + "configured. Configure it with a db-kind, or point quarkus.modulith.events.jdbc.datasource to a "
                + "configured datasource.");
    }
}
