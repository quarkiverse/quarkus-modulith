package io.quarkiverse.modulith.events.jdbc.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

/**
 * Creates the {@link JdbcStore} from the configured datasource and creates the tables at startup when asked to.
 */
@Singleton
public class JdbcStoreProducer {

    @Produces
    @Singleton
    JdbcStore store(@Any Instance<AgroalDataSource> dataSources, JdbcDatabase database, EventSerializer serializer,
            JdbcBuildTimeConfig buildTimeConfig, JdbcConfig config, EventsFixedConfig events) {
        AgroalDataSource dataSource = buildTimeConfig.datasource()
                .map(name -> dataSources.select(new DataSource.DataSourceLiteral(name)))
                .orElseGet(() -> dataSources.select(Default.Literal.INSTANCE))
                .get();
        return JdbcStore.create(dataSource, database.type(), serializer, config.schema(),
                buildTimeConfig.useLegacyStructure(), events.completionMode());
    }

    /**
     * Runs before the registry's own startup work, so republishing at startup finds the tables. Resolving the store
     * here also detects an unsupported database at startup.
     */
    void initialize(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) StartupEvent event, JdbcStore store,
            JdbcConfig config) {
        if (config.schemaInitialization().enabled()) {
            new DatabaseSchemaInitializer(store).initialize();
        }
    }
}
