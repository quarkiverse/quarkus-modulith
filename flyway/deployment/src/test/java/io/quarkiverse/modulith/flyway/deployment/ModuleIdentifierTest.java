package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.util.ClassPathUtils;

class ModuleIdentifierTest {

    @ParameterizedTest
    @ValueSource(strings = { "order.internal", "1st", "with space", "x; DROP TABLE t",
            "a2345678901234567890123456789012345678901" })
    void rejectsIdentifiersThatCannotSuffixATableName(String module) {
        // act
        Throwable thrown = catchThrowable(() -> FlywayProcessor.checkIdentifiers(List.of("billing", module),
                it -> true));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Modules [" + module + "] ");
    }

    @ParameterizedTest
    @ValueSource(strings = { "billing", "Order_2", "a234567890123456789012345678901234567890" })
    void acceptsPlainIdentifiers(String module) {
        // act
        Throwable thrown = catchThrowable(() -> FlywayProcessor.checkIdentifiers(List.of(module), it -> true));

        // assert
        assertThat(thrown).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "shipping.tracking", "monthly-reports" })
    void acceptsAnyIdentifierOfAModuleWithoutMigrations(String module) {
        // act
        Throwable thrown = catchThrowable(() -> FlywayProcessor.checkIdentifiers(List.of(module), "billing"::equals));

        // assert
        assertThat(thrown).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "db/migration, db/migration/", "classpath:db/migration, db/migration/",
            "classpath:/db/migration/, db/migration/", "classpath:, ''", "/, ''" })
    void findsTheClassPathFolderOfALocation(String location, String folder) {
        // act
        Optional<String> found = FlywayProcessor.classPathFolder(location);

        // assert
        assertThat(found).hasValue(folder);
    }

    @Test
    void leavesFileSystemLocationsOut() {
        // act
        Optional<String> found = FlywayProcessor.classPathFolder("filesystem:/opt/migrations");

        // assert
        assertThat(found).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "shipping, true", "reports, true", "audit, false" })
    void looksForMigrationsOnTheClassPath(String module, boolean expected) {
        // arrange
        ClassLoader loader = ModuleIdentifierTest.class.getClassLoader();
        List<String> locations = List.of("filesystem:/opt/migrations", "classpath:db/migration");

        // act
        boolean found = FlywayProcessor.hasMigrations(locations, module,
                (folder, consumer) -> ClassPathUtils.consumeAsPaths(loader, folder, consumer));

        // assert
        assertThat(found).isEqualTo(expected);
    }

    @Test
    void failsWhenTheClassPathCannotBeRead() {
        // act
        Throwable thrown = catchThrowable(() -> FlywayProcessor.hasFiles("db/migration/shipping", (folder, consumer) -> {
            throw new IOException("unreadable");
        }));

        // assert
        assertThat(thrown).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot read the Flyway migrations in db/migration/shipping");
    }

    @Test
    void failsWhenAFolderCannotBeRead() {
        // arrange
        Path missing = Path.of("does-not-exist");

        // act
        Throwable thrown = catchThrowable(() -> FlywayProcessor.containsFile(missing));

        // assert
        assertThat(thrown).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot read the Flyway migrations in does-not-exist");
    }
}
