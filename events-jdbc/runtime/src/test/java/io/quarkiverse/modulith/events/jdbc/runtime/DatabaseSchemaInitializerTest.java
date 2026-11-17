package io.quarkiverse.modulith.events.jdbc.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DatabaseSchemaInitializerTest {

    @Test
    void splitsScriptsIntoStatementsWithoutComments() {
        // arrange
        String script = """
                -- a comment
                CREATE TABLE A (ID INT);
                  -- an indented comment
                CREATE INDEX A_IDX ON A (ID);

                """;

        // act
        List<String> statements = DatabaseSchemaInitializer.statements(script);

        // assert
        assertThat(statements).containsExactly("CREATE TABLE A (ID INT)", "CREATE INDEX A_IDX ON A (ID)");
    }

    @Test
    void keepsAScriptWithoutSemicolonAsOneStatement() {
        // arrange
        String script = "IF NOT EXISTS(SELECT 1) CREATE TABLE A (ID INT)";

        // act
        List<String> statements = DatabaseSchemaInitializer.statements(script);

        // assert
        assertThat(statements).containsExactly(script);
    }

    @ParameterizedTest
    @CsvSource({ "true, false, EVENTS", "false, true, events", "false, false, Events" })
    void writesIdentifiersAsTheDatabaseStoresThem(boolean upperCase, boolean lowerCase, String expected) {
        // act
        String identifier = DatabaseSchemaInitializer.identifier("Events", upperCase, lowerCase);

        // assert
        assertThat(identifier).isEqualTo(expected);
    }

    @Test
    void reportsAMissingSchemaFile() {
        // act
        Throwable thrown = catchThrowable(() -> DatabaseSchemaInitializer.read("db/modulith/missing.sql"));

        // assert
        assertThat(thrown).isInstanceOf(FileNotFoundException.class)
                .hasMessage("Missing schema resource db/modulith/missing.sql");
    }
}
