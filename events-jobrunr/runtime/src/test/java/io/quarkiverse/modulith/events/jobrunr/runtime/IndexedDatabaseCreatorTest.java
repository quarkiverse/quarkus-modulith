package io.quarkiverse.modulith.events.jobrunr.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.sql.common.DatabaseCreator;
import org.jobrunr.storage.sql.common.DatabaseMigrationsProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexedDatabaseCreatorTest {

    private static final String COMMON = IndexedDatabaseCreator.COMMON_MIGRATIONS;
    private static final String COMMON_JOB_TABLE = COMMON + "v001__create_job_table.sql";
    private static final String COMMON_STATS = COMMON + "v010__change_job_stats.sql";
    private static final String STORAGE = "org/jobrunr/storage/sql/";
    private static final String H2_STATS = STORAGE + "h2/migrations/v010__change_job_stats.sql";
    private static final String POSTGRES_STATS = STORAGE + "postgres/migrations/v014__improve_job_stats.sql";

    private final JdbcDataSource h2 = new JdbcDataSource();
    private Connection keeper;

    @BeforeEach
    void openDatabase() throws SQLException {
        h2.setURL("jdbc:h2:mem:indexed");
        h2.setUser("sa");
        h2.setPassword("");
        keeper = h2.getConnection();
    }

    @AfterEach
    void dropDatabase() throws SQLException {
        try (Statement statement = keeper.createStatement()) {
            statement.execute("drop all objects");
        }
        keeper.close();
    }

    /**
     * @return the path of every SQL migration in JobRunr's jar, as the build indexes them
     */
    private static List<String> jobRunrMigrations() throws IOException, URISyntaxException {
        Path jar = Path.of(DatabaseCreator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarFile entries = new JarFile(jar.toFile())) {
            return entries.stream().map(ZipEntry::getName)
                    .filter(name -> name.matches("org/jobrunr/storage/sql/[^/]+/migrations/[^/]+\\.sql")).toList();
        }
    }

    private static ClassLoader indexing(List<String> paths) {
        return serving(Map.of(IndexedDatabaseCreator.INDEX, String.join("\n", paths)));
    }

    private static ClassLoader serving(Map<String, String> resources) {
        return new ClassLoader(IndexedDatabaseCreatorTest.class.getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return resources.containsKey(name) ? new ByteArrayInputStream(resources.get(name).getBytes(UTF_8))
                        : super.getResourceAsStream(name);
            }
        };
    }

    private static Map<String, String> sqlByName(List<SqlMigration> migrations) {
        return migrations.stream().collect(Collectors.toMap(SqlMigration::getFileName, migration -> {
            try {
                return migration.getMigrationSql();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    private List<String> tables() throws SQLException {
        try (ResultSet rows = keeper.getMetaData().getTables(null, null, "JOBRUNR_%", null)) {
            List<String> names = new ArrayList<>();
            while (rows.next()) {
                names.add(rows.getString("TABLE_NAME"));
            }
            return names;
        }
    }

    @Test
    void takesTheSameMigrationsAsJobRunrListsOnTheClassPath() throws Exception {
        // arrange
        ClassLoader resources = indexing(jobRunrMigrations());
        Map<String, String> listedByJobRunr = sqlByName(
                new DatabaseMigrationsProvider(H2StorageProvider.class).getMigrations().toList());

        // act
        List<SqlMigration> migrations = IndexedDatabaseCreator.migrations(H2StorageProvider.class, resources);

        // assert
        assertThat(sqlByName(migrations)).isEqualTo(listedByJobRunr);
        assertThat(migrations).extracting(SqlMigration::getFileName).isSorted();
    }

    @Test
    void replacesACommonMigrationWithTheOneOfTheDatabase() throws IOException {
        // arrange
        ClassLoader resources = indexing(List.of(H2_STATS, COMMON_STATS, POSTGRES_STATS, COMMON_JOB_TABLE));

        // act
        List<SqlMigration> migrations = IndexedDatabaseCreator.migrations(H2StorageProvider.class, resources);

        // assert
        assertThat(migrations).extracting(SqlMigration::getFileName)
                .containsExactly("v001__create_job_table.sql", "v010__change_job_stats.sql");
        assertThat(migrations.get(1).getMigrationSql())
                .isEqualTo(IndexedDatabaseCreator.read(getClass().getClassLoader(), H2_STATS));
    }

    @Test
    void createsJobRunrsTablesFromTheIndex() throws Exception {
        // arrange
        IndexedDatabaseCreator creator = new IndexedDatabaseCreator(h2, null, H2StorageProvider.class,
                indexing(jobRunrMigrations()));

        // act
        creator.runMigrations();

        // assert
        assertThat(tables()).contains("JOBRUNR_JOBS", "JOBRUNR_RECURRING_JOBS", "JOBRUNR_BACKGROUNDJOBSERVERS",
                "JOBRUNR_METADATA", "JOBRUNR_MIGRATIONS");
        creator.validateTables();
    }

    @Test
    void failsWhenTheBuildWroteNoIndex() {
        // arrange
        ClassLoader resources = serving(Collections.emptyMap());

        // act + assert
        assertThatThrownBy(() -> IndexedDatabaseCreator.migrations(H2StorageProvider.class, resources))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(IndexedDatabaseCreator.INDEX);
    }

    @Test
    void failsWhenAMigrationCannotBeRead() {
        // arrange
        ClassLoader resources = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("disk gone");
                    }
                };
            }
        };

        // act + assert
        assertThatThrownBy(() -> IndexedDatabaseCreator.read(resources, COMMON_JOB_TABLE))
                .isInstanceOf(UncheckedIOException.class).hasMessageContaining(COMMON_JOB_TABLE)
                .hasRootCauseMessage("disk gone");
    }

    @Test
    void refusesMissingTablesWhenTheirCreationIsSkipped() throws Exception {
        // arrange
        IndexedDatabaseCreator creator = new IndexedDatabaseCreator(h2, null, H2StorageProvider.class,
                indexing(jobRunrMigrations()));

        // act + assert
        assertThatThrownBy(() -> TransactionalStorageProviderProducer.setUp(creator, true))
                .isInstanceOf(JobRunrException.class);
        assertThat(tables()).isEmpty();
    }

    @Test
    void createsTheTablesUnlessTheirCreationIsSkipped() throws Exception {
        // arrange
        IndexedDatabaseCreator creator = new IndexedDatabaseCreator(h2, null, H2StorageProvider.class,
                indexing(jobRunrMigrations()));

        // act
        TransactionalStorageProviderProducer.setUp(creator, false);
        TransactionalStorageProviderProducer.setUp(creator, true);

        // assert
        assertThat(tables()).contains("JOBRUNR_JOBS");
    }
}
