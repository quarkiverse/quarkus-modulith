package io.quarkiverse.modulith.events.jobrunr.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.quarkiverse.modulith.events.jobrunr.runtime.IndexedDatabaseCreator;
import io.quarkus.runtime.util.ClassPathUtils;

/**
 * Lists JobRunr's SQL migrations at build time for the index {@link IndexedDatabaseCreator} reads, since a native image
 * cannot list them on the class path.
 */
final class JobRunrMigrations {

    static final String SQL_STORAGE = "org/jobrunr/storage/sql";
    private static final String MIGRATIONS = "migrations";
    private static final String SQL = ".sql";

    private JobRunrMigrations() {
    }

    /**
     * @return the class path of every SQL file in a {@code migrations} folder of JobRunr's SQL storage
     */
    static List<String> list(ClassLoader classLoader) {
        List<String> migrations = new ArrayList<>();
        try {
            ClassPathUtils.consumeAsPaths(classLoader, SQL_STORAGE, root -> migrations.addAll(list(root)));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list JobRunr's SQL migrations", e);
        }
        return migrations;
    }

    /**
     * @return the path under {@value #SQL_STORAGE} of every SQL migration below {@code root}, which stands for that
     *         folder
     */
    static List<String> list(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(JobRunrMigrations::isMigration).sorted()
                    .map(file -> SQL_STORAGE + "/" + root.relativize(file).toString().replace('\\', '/')).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list the SQL migrations in " + root, e);
        }
    }

    private static boolean isMigration(Path file) {
        return file.getFileName().toString().endsWith(SQL)
                && MIGRATIONS.equals(file.getParent().getFileName().toString());
    }
}
