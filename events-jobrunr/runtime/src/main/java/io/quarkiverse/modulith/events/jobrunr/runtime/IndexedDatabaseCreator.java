package io.quarkiverse.modulith.events.jobrunr.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.DatabaseCreator;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.migrations.SqlMigrationByZipEntry;

/**
 * Creates and validates JobRunr's tables like JobRunr's {@link DatabaseCreator}, but takes the SQL migrations from the
 * index the build writes instead of listing the migration folders on the class path. In a native image JobRunr 9.0.4
 * lists them through the {@code resource:/resources} file system, which GraalVM 25 rejects, so the application does not
 * start. Like JobRunr, a migration of the database's own folder replaces the common migration of the same name.
 */
public class IndexedDatabaseCreator extends DatabaseCreator {

    /**
     * The class path resource listing every JobRunr SQL migration, one path per line.
     */
    public static final String INDEX = "META-INF/quarkus-modulith/jobrunr-migrations.txt";
    static final String COMMON_MIGRATIONS = "org/jobrunr/storage/sql/common/migrations/";
    private static final String MIGRATIONS = "/migrations/";

    private final Class<? extends SqlStorageProvider> storageProvider;
    private final ClassLoader resources;

    IndexedDatabaseCreator(DataSource dataSource, String tablePrefix,
            Class<? extends SqlStorageProvider> storageProvider, ClassLoader resources) {
        super(dataSource, tablePrefix, storageProvider);
        this.storageProvider = storageProvider;
        this.resources = resources;
    }

    @Override
    protected Stream<SqlMigration> getMigrations() {
        return migrations(storageProvider, resources).stream();
    }

    /**
     * @return the common migrations and those of the storage provider's database, which replace the common ones of the
     *         same name
     */
    static List<SqlMigration> migrations(Class<?> storageProvider, ClassLoader resources) {
        String own = storageProvider.getPackageName().replace('.', '/') + MIGRATIONS;
        List<String> index = read(resources, INDEX).lines().toList();
        Map<String, String> byName = new TreeMap<>();
        index.stream().filter(path -> path.startsWith(COMMON_MIGRATIONS)).forEach(path -> byName.put(name(path), path));
        index.stream().filter(path -> path.startsWith(own)).forEach(path -> byName.put(name(path), path));
        return byName.values().stream()
                .<SqlMigration> map(path -> new SqlMigrationByZipEntry(path, read(resources, path))).toList();
    }

    static String read(ClassLoader resources, String path) {
        try (InputStream content = resources.getResourceAsStream(path)) {
            if (content == null) {
                throw new IllegalStateException("The class path has no " + path
                        + ", which the build of quarkus-modulith-events-jobrunr writes for JobRunr's tables");
            }
            return new String(content.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    private static String name(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
