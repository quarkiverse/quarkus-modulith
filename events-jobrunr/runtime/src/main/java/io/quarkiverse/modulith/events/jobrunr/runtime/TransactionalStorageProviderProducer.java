package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.sql.DataSource;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.DatabaseCreator;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;

/**
 * Replaces the SQL storage provider of JobRunr's Quarkus extension with one on a {@link TransactionalDataSource}, like
 * Spring Modulith's {@code ModulithJobRunrSqlStorageProviderAutoConfiguration}. It reads the same
 * {@code quarkus.jobrunr.database.*} settings. It builds the storage provider without touching the tables and then
 * creates them with an {@link IndexedDatabaseCreator}, whose migrations a native image can read.
 * <p>
 * It relies on internals of JobRunr's Quarkus extension ({@code JobRunrRuntimeConfiguration} and the
 * {@code @DefaultBean} storage provider it replaces), tested with {@code org.jobrunr:quarkus-jobrunr} 9.0.4 on
 * Quarkus 3.40 and 4.0.0.CR1. Check this class, and run {@code integration-tests/externalization-jobrunr}, when
 * upgrading JobRunr.
 */
@Singleton
public class TransactionalStorageProviderProducer {

    @Produces
    @Singleton
    StorageProvider storageProvider(@Any Instance<DataSource> dataSources, JobMapper jobMapper,
            JobRunrRuntimeConfiguration configuration, TransactionSynchronizationRegistry transactions) {
        JobRunrRuntimeConfiguration.DatabaseConfiguration database = configuration.database();
        DataSource dataSource = new TransactionalDataSource(
                dataSources.select(DataSource.class, qualifier(database.datasource())).get(), transactions);
        String tablePrefix = database.tablePrefix().orElse(null);
        StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource, tablePrefix,
                DatabaseOptions.NO_VALIDATE);
        setUp(new IndexedDatabaseCreator(dataSource, tablePrefix, storageProvider.getClass().asSubclass(
                SqlStorageProvider.class), Thread.currentThread().getContextClassLoader()), database.skipCreate());
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    /**
     * Creates JobRunr's tables, or only checks that they exist when {@code quarkus.jobrunr.database.skip-create} is
     * set, as JobRunr's SQL storage provider does on its own construction.
     */
    static void setUp(DatabaseCreator tables, boolean skipCreate) {
        if (skipCreate) {
            tables.validateTables();
        } else {
            tables.runMigrations();
        }
    }

    /**
     * @return the qualifier JobRunr's own producer selects the data source with
     */
    static Annotation qualifier(Optional<String> name) {
        return name.<Annotation> map(NamedLiteral::of).orElse(Default.Literal.INSTANCE);
    }
}
