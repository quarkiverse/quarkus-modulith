package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.modulith.flyway.runtime.FlywayModules;
import io.quarkus.test.QuarkusExtensionTest;

public class NoModelMigrationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MigrationLog.class)
                    .addAsResource("flyway/root.sql", "db/migration/__root/V1__log.sql"))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:nomodel;DB_CLOSE_DELAY=-1");

    @Inject
    AgroalDataSource dataSource;

    @Inject
    FlywayModules modules;

    @Test
    void migratesTheRootOnly() throws SQLException {
        // act
        List<String> entries = MigrationLog.entries(dataSource);

        // assert
        assertThat(entries).containsExactly("root");
        assertThat(modules.identifiers()).isEmpty();
    }
}
