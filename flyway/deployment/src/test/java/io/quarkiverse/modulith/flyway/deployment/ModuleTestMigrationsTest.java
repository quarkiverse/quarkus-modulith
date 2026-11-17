package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleTestMigrationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(ShopApplication.archive())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", ShopApplication.BASE_PACKAGE)
            .overrideConfigKey("quarkus.modulith.test.module", "billing")
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:billing;DB_CLOSE_DELAY=-1");

    @Inject
    AgroalDataSource dataSource;

    @Test
    void migratesOnlyTheModulesOfTheTest() throws SQLException {
        // act
        List<String> entries = MigrationLog.entries(dataSource);
        List<String> historyTables = MigrationLog.historyTables(dataSource);

        // assert
        assertThat(entries).containsExactly("root", "billing");
        assertThat(historyTables).containsExactly("flyway_schema_history", "flyway_schema_history_billing");
    }
}
