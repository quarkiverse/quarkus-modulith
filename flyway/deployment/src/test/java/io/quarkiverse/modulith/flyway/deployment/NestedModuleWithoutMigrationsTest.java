package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.modulith.flyway.deployment.fixtures.nested.shipping.Shipments;
import io.quarkiverse.modulith.flyway.deployment.fixtures.nested.shipping.tracking.Tracker;
import io.quarkiverse.modulith.flyway.runtime.FlywayModules;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The nested module {@code shipping.tracking} keeps its default identifier, which cannot suffix a table name, and
 * starts, because it has no migrations and therefore gets no history table.
 */
public class NestedModuleWithoutMigrationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Shipments.class.getPackage())
                    .addPackage(Tracker.class.getPackage()).addClass(MigrationLog.class)
                    .addAsResource("flyway/root.sql", "db/migration/__root/V1__log.sql")
                    .addAsResource("flyway/shipping.sql", "db/migration/shipping/V1__shipping.sql"))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.flyway.deployment.fixtures.nested")
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:nested;DB_CLOSE_DELAY=-1");

    @Inject
    AgroalDataSource dataSource;

    @Inject
    FlywayModules modules;

    @Test
    void migratesTheModulesWithMigrationsOnly() throws SQLException {
        // act
        List<String> historyTables = MigrationLog.historyTables(dataSource);

        // assert
        assertThat(modules.identifiers()).containsExactly("shipping", "shipping.tracking");
        assertThat(historyTables).containsExactly("flyway_schema_history", "flyway_schema_history_shipping");
        assertThat(MigrationLog.entries(dataSource)).containsExactly("root", "shipping");
    }
}
