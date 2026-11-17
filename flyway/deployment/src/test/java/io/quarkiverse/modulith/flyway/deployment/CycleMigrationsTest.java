package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.modulith.flyway.deployment.fixtures.cycle.alpha.Alpha;
import io.quarkiverse.modulith.flyway.deployment.fixtures.cycle.beta.Beta;
import io.quarkus.test.QuarkusExtensionTest;

public class CycleMigrationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Alpha.class, Beta.class, MigrationLog.class)
                    .addAsResource("flyway/root.sql", "db/migration/__root/V1__log.sql")
                    .addAsResource("flyway/beta.sql", "db/migration/beta/V1__beta.sql")
                    .addAsResource("flyway/alpha.sql", "db/migration/alpha/V1__alpha.sql"))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.flyway.deployment.fixtures.cycle")
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn")
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:cycle;DB_CLOSE_DELAY=-1");

    @Inject
    AgroalDataSource dataSource;

    @Test
    void migratesTheCycleInModelOrder() throws SQLException {
        // act
        List<String> entries = MigrationLog.entries(dataSource);

        // assert
        assertThat(entries).containsExactly("root", "alpha", "beta");
    }
}
