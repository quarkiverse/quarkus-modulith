package io.quarkiverse.modulith.events.jdbc.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DatabaseTypeTest {

    private static final UUID ID = UUID.fromString("6f1c9a52-1b0e-4c8e-9f55-2b7d7c1f3a10");

    @ParameterizedTest
    @CsvSource({
            "H2, H2",
            "HSQL Database Engine, HSQLDB",
            "postgresql, POSTGRESQL",
            "MySQL, MYSQL",
            "MariaDB, MARIADB",
            "Microsoft SQL Server, SQLSERVER",
            "Oracle, ORACLE" })
    void detectsTheDatabaseByProductName(String productName, DatabaseType expected) {
        // act
        DatabaseType type = DatabaseType.fromProductName(productName);

        // assert
        assertThat(type).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "DB2/LINUXX8664", "Apache Derby" })
    void rejectsOtherDatabases(String productName) {
        // act
        Throwable thrown = catchThrowable(() -> DatabaseType.fromProductName(productName));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unsupported database for the JDBC event publication registry: " + productName
                        + ". Supported: H2, HSQL Database Engine, PostgreSQL, MySQL, MariaDB, Microsoft SQL Server, Oracle");
    }

    @ParameterizedTest
    @CsvSource({
            "H2, h2, true, false, ' LIMIT 3'",
            "HSQLDB, hsqldb, true, false, ' LIMIT 3'",
            "POSTGRESQL, postgresql, true, false, ' LIMIT 3'",
            "MYSQL, mysql, false, true, ' LIMIT 3'",
            "MARIADB, mariadb, false, true, ' LIMIT 3'",
            "SQLSERVER, sqlserver, false, true, ' OFFSET 0 ROWS FETCH NEXT 3 ROWS ONLY'",
            "ORACLE, oracle, false, true, ' FETCH FIRST 3 ROWS ONLY'" })
    void describesEachDatabase(DatabaseType type, String value, boolean schemaSupported, boolean uuidAsString,
            String limitClause) {
        // act
        String limit = type.limitClause(3);

        // assert
        assertThat(type.value()).isEqualTo(value);
        assertThat(type.isSchemaSupported()).isEqualTo(schemaSupported);
        assertThat(limit).isEqualTo(limitClause);
        assertThat(type.uuidToDatabase(ID)).isEqualTo(uuidAsString ? ID.toString() : ID);
    }

    @ParameterizedTest
    @EnumSource(DatabaseType.class)
    void shipsAllFourSchemaFiles(DatabaseType type) throws IOException {
        // act
        String v1 = DatabaseSchemaInitializer.read(type.schemaResource(true));
        String v2 = DatabaseSchemaInitializer.read(type.schemaResource(false));
        String v1Archive = DatabaseSchemaInitializer.read(type.archiveSchemaResource(true));
        String v2Archive = DatabaseSchemaInitializer.read(type.archiveSchemaResource(false));

        // assert
        assertThat(type.schemaResource(false)).isEqualTo("db/modulith/events/jdbc/v2/schema-" + type.value() + ".sql");
        assertThat(type.archiveSchemaResource(true))
                .isEqualTo("db/modulith/events/jdbc/v1/schema-" + type.value() + "-archive.sql");
        assertThat(v1).containsIgnoringCase("EVENT_PUBLICATION").doesNotContainIgnoringCase("COMPLETION_ATTEMPTS");
        assertThat(v2).containsIgnoringCase("COMPLETION_ATTEMPTS");
        assertThat(v1Archive).containsIgnoringCase("EVENT_PUBLICATION_ARCHIVE");
        assertThat(v2Archive).containsIgnoringCase("EVENT_PUBLICATION_ARCHIVE").containsIgnoringCase("STATUS");
    }

    @ParameterizedTest
    @CsvSource({ "6f1c9a52-1b0e-4c8e-9f55-2b7d7c1f3a10", "6F1C9A52-1B0E-4C8E-9F55-2B7D7C1F3A10" })
    void readsIdentifiersStoredAsUuidOrText(String text) {
        // act
        UUID fromUuid = DatabaseType.databaseToUuid(ID);
        UUID fromText = DatabaseType.databaseToUuid(text);

        // assert
        assertThat(fromUuid).isSameAs(ID);
        assertThat(fromText).isEqualTo(ID);
    }
}
