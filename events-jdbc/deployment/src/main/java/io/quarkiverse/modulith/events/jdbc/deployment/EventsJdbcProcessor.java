package io.quarkiverse.modulith.events.jdbc.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseType;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcBuildTimeConfig;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcDatabase;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcDatabaseCreator;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcEventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcStoreProducer;
import io.quarkiverse.modulith.events.jdbc.runtime.LegacyJdbcEventPublicationRepository;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Build steps of {@code quarkus-modulith-events-jdbc}: registers the repository for the configured table structure,
 * fixes the database from the datasource's {@code db-kind}, checks the datasource and ships the schema files in
 * native executables.
 */
class EventsJdbcProcessor {

    static final String FEATURE = "modulith-events-jdbc";

    /**
     * The {@code db-kind} values with Spring Modulith schema files. {@code other} is detected from the connection.
     */
    static final Map<String, DatabaseType> DB_KINDS = Map.of(
            "h2", DatabaseType.H2,
            "postgresql", DatabaseType.POSTGRESQL,
            "mysql", DatabaseType.MYSQL,
            "mariadb", DatabaseType.MARIADB,
            "mssql", DatabaseType.SQLSERVER,
            "oracle", DatabaseType.ORACLE);

    static final String OTHER = "other";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * The repository class depends on the table structure, the other beans are always there.
     */
    @BuildStep
    void beans(JdbcBuildTimeConfig config, List<JdbcDataSourceBuildItem> dataSources,
            BuildProducer<AdditionalBeanBuildItem> beans, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        Class<?> repository = config.useLegacyStructure() ? LegacyJdbcEventPublicationRepository.class
                : JdbcEventPublicationRepository.class;
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(JdbcStoreProducer.class, repository)
                .build());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(JdbcDatabase.class)
                .scope(Singleton.class)
                .creator(JdbcDatabaseCreator.class)
                .param(JdbcDatabaseCreator.TYPE, databaseType(dbKind(config, dataSources)).map(DatabaseType::name)
                        .orElse(""))
                .done());
    }

    /**
     * Fails the build when the datasource the registry is configured for does not exist or has a database without
     * schema files.
     */
    @BuildStep
    void checkDataSource(JdbcBuildTimeConfig config, List<JdbcDataSourceBuildItem> dataSources,
            ValidationPhaseBuildItem validationPhase, BuildProducer<ValidationErrorBuildItem> errors) {
        problem(config.datasource(), dbKind(config, dataSources))
                .ifPresent(message -> errors.produce(new ValidationErrorBuildItem(new ConfigurationException(message))));
    }

    /**
     * The schema initializer reads the table definitions from the class path at runtime: those of the datasource's
     * database, or of every database when it is only known at startup, for the table structure fixed at build time,
     * with the archive table only in archive completion mode.
     */
    @BuildStep
    NativeImageResourceBuildItem schemas(JdbcBuildTimeConfig config, EventsFixedConfig events,
            List<JdbcDataSourceBuildItem> dataSources) {
        return schemaResources(databaseType(dbKind(config, dataSources)), config.useLegacyStructure(),
                events.completionMode() == CompletionMode.ARCHIVE);
    }

    static NativeImageResourceBuildItem schemaResources(Optional<DatabaseType> type, boolean legacy, boolean archive) {
        List<String> resources = type.map(Stream::of).orElseGet(() -> Arrays.stream(DatabaseType.values()))
                .flatMap(database -> archive
                        ? Stream.of(database.schemaResource(legacy), database.archiveSchemaResource(legacy))
                        : Stream.of(database.schemaResource(legacy)))
                .toList();
        return new NativeImageResourceBuildItem(resources);
    }

    /**
     * @return the {@code db-kind} of the registry's datasource, empty when the datasource is not configured
     */
    static Optional<String> dbKind(JdbcBuildTimeConfig config, List<JdbcDataSourceBuildItem> dataSources) {
        return dataSources.stream()
                .filter(dataSource -> config.datasource().map(dataSource.getName()::equals)
                        .orElseGet(dataSource::isDefault))
                .map(JdbcDataSourceBuildItem::getDbKind)
                .findFirst();
    }

    /**
     * @return the database of a supported {@code db-kind}, empty for {@code other} and unsupported kinds
     */
    static Optional<DatabaseType> databaseType(Optional<String> dbKind) {
        return dbKind.map(DB_KINDS::get);
    }

    /**
     * @return what is wrong with the registry's datasource, if anything
     */
    static Optional<String> problem(Optional<String> name, Optional<String> dbKind) {
        String dataSource = name.map(value -> "the datasource '" + value + "'").orElse("the default datasource");
        if (dbKind.isEmpty()) {
            return Optional.of("The JDBC event publication registry uses " + dataSource + ", which is not configured. "
                    + "Configure it with a db-kind, or point quarkus.modulith.events.jdbc.datasource to a configured "
                    + "datasource.");
        }
        String kind = dbKind.get();
        if (kind.equals(OTHER) || DB_KINDS.containsKey(kind)) {
            return Optional.empty();
        }
        return Optional.of("The JDBC event publication registry has no schema for db-kind '" + kind + "' of "
                + dataSource + ". Supported: " + String.join(", ", DB_KINDS.keySet().stream().sorted().toList())
                + ", and other for HSQLDB.");
    }
}
