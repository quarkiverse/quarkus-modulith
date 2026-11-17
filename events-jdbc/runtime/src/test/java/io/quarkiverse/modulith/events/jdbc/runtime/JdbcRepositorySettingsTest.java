package io.quarkiverse.modulith.events.jdbc.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;

class JdbcRepositorySettingsTest {

    @ParameterizedTest
    @CsvSource({
            "UPDATE, EVENT_PUBLICATION, false",
            "DELETE, EVENT_PUBLICATION, false",
            "ARCHIVE, EVENT_PUBLICATION_ARCHIVE, true" })
    void namesTheTablesWithoutSchema(CompletionMode completion, String completedTable, boolean archive) {
        // act
        JdbcRepositorySettings settings = new JdbcRepositorySettings(DatabaseType.MYSQL, Optional.empty(), false,
                completion);

        // assert
        assertThat(settings.table()).isEqualTo("EVENT_PUBLICATION");
        assertThat(settings.archiveTable()).isEqualTo("EVENT_PUBLICATION_ARCHIVE");
        assertThat(settings.completedTable()).isEqualTo(completedTable);
        assertThat(settings.isArchive()).isEqualTo(archive);
    }

    @Test
    void qualifiesTheTablesWithTheSchema() {
        // act
        JdbcRepositorySettings settings = new JdbcRepositorySettings(DatabaseType.POSTGRESQL, Optional.of("events"),
                true, CompletionMode.ARCHIVE);

        // assert
        assertThat(settings.table()).isEqualTo("events.EVENT_PUBLICATION");
        assertThat(settings.completedTable()).isEqualTo("events.EVENT_PUBLICATION_ARCHIVE");
        assertThat(settings.legacyStructure()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseType.class, names = { "MYSQL", "MARIADB", "SQLSERVER", "ORACLE" })
    void rejectsASchemaWhereSpringModulithDoes(DatabaseType type) {
        // act
        Throwable thrown = catchThrowable(
                () -> new JdbcRepositorySettings(type, Optional.of("events"), false, CompletionMode.UPDATE));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("quarkus.modulith.events.jdbc.schema is not supported on " + type.name()
                        + ". Remove the property, or use H2, HSQLDB or PostgreSQL.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "x; DROP TABLE ORDERS --",
            "public.EVENT_PUBLICATION WHERE 1=1 OR",
            "events\"",
            "events--",
            "1events",
            "ev ents",
            "",
            "a234567890123456789012345678901234567890123456789012345678901234" })
    void rejectsSchemaNamesThatAreNotPlainIdentifiers(String schema) {
        // act
        Throwable thrown = catchThrowable(
                () -> new JdbcRepositorySettings(DatabaseType.POSTGRESQL, Optional.of(schema), false, CompletionMode.UPDATE));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("quarkus.modulith.events.jdbc.schema must be an unquoted SQL identifier (a letter or "
                        + "underscore, then up to 62 letters, digits or underscores), but is: " + schema);
    }

    @ParameterizedTest
    @ValueSource(strings = { "events", "_outbox", "Events_2026",
            "a23456789012345678901234567890123456789012345678901234567890123" })
    void acceptsPlainIdentifiersAsSchemaNames(String schema) {
        // act
        JdbcRepositorySettings settings = new JdbcRepositorySettings(DatabaseType.H2, Optional.of(schema), false,
                CompletionMode.UPDATE);

        // assert
        assertThat(settings.table()).isEqualTo(schema + ".EVENT_PUBLICATION");
    }

}
