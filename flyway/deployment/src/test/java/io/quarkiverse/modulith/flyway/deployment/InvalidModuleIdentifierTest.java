package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.flyway.deployment.fixtures.badid.reports.Reports;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidModuleIdentifierTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Reports.class.getPackage())
                    .addAsResource("flyway/billing.sql", "db/migration/monthly-reports/V1__reports.sql"))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.flyway.deployment.fixtures.badid")
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:badid")
            .assertException(thrown -> assertThat(thrown).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Modules [monthly-reports] cannot get their own Flyway history table: a module identifier "
                            + "used with quarkus.modulith.flyway.enabled must start with a letter and have at most 40 "
                            + "letters, digits or underscores. Change it with @ApplicationModule(id = ...)."));

    @Test
    void failsTheBuild() {
    }
}
