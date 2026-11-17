package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleMigrationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(ShopApplication.archive()
                    .andThen(jar -> jar.addClasses(RecordingCustomizer.class, TableRecorder.class)))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", ShopApplication.BASE_PACKAGE)
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:shop;DB_CLOSE_DELAY=-1");

    @Inject
    AgroalDataSource dataSource;

    @Inject
    RecordingCustomizer recorder;

    @Test
    void migratesTheRootThenEachModuleInDependencyOrder() throws SQLException {
        // act
        List<String> entries = MigrationLog.entries(dataSource);
        List<String> historyTables = MigrationLog.historyTables(dataSource);

        // assert
        assertThat(entries).containsExactly("root", "reports-java", "shipping", "shipping-java", "billing");
        assertThat(historyTables).containsExactly("flyway_schema_history", "flyway_schema_history_billing",
                "flyway_schema_history_reports", "flyway_schema_history_shipping");
        assertThat(recorder.tables).containsExactlyInAnyOrder("flyway_schema_history_reports",
                "flyway_schema_history_shipping", "flyway_schema_history_billing", "flyway_schema_history");
    }
}
