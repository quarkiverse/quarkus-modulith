package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobRunrMigrationsTest {

    private static final String STORAGE = JobRunrMigrations.SQL_STORAGE + "/";

    @TempDir
    Path root;

    private void write(String path) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "select 1");
    }

    @Test
    void listsTheMigrationsInJobRunrsJar() {
        // act
        List<String> migrations = JobRunrMigrations.list(getClass().getClassLoader());

        // assert
        assertThat(migrations).contains(STORAGE + "common/migrations/v000__create_migrations_table.sql",
                STORAGE + "h2/migrations/v010__change_job_stats.sql",
                STORAGE + "postgres/migrations/v014__improve_job_stats.sql")
                .allMatch(path -> path.matches(STORAGE + "[^/]+/migrations/[^/]+\\.sql"));
    }

    @Test
    void keepsOnlyTheSqlFilesOfMigrationFolders() throws IOException {
        // arrange
        write("h2/migrations/v002__b.sql");
        write("common/migrations/v001__a.sql");
        write("common/migrations/SqlMigration.class");
        write("common/scripts/v001__not_a_migration.sql");

        // act
        List<String> migrations = JobRunrMigrations.list(root);

        // assert
        assertThat(migrations).containsExactly(STORAGE + "common/migrations/v001__a.sql",
                STORAGE + "h2/migrations/v002__b.sql");
    }

    @Test
    void failsOnAFolderThatCannotBeWalked() {
        // arrange
        Path missing = root.resolve("missing");

        // act + assert
        assertThatThrownBy(() -> JobRunrMigrations.list(missing)).isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining(missing.toString());
    }

    @Test
    void failsOnAClassPathThatCannotBeRead() {
        // arrange
        ClassLoader broken = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                throw new IOException("class path gone");
            }
        };

        // act + assert
        assertThatThrownBy(() -> JobRunrMigrations.list(broken)).isInstanceOf(UncheckedIOException.class)
                .hasRootCauseMessage("class path gone");
    }
}
