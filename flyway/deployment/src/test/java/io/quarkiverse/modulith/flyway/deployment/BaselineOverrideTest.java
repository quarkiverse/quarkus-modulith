package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.flyway.runtime.ModulithFlywayCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

class BaselineOverrideTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(ShopApplication.archive())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", ShopApplication.BASE_PACKAGE)
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway.baseline-version", "5")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:baseline;DB_CLOSE_DELAY=-1")
            .setLogRecordPredicate(r -> ModulithFlywayCustomizer.class.getName().equals(r.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).singleElement().satisfies(r -> {
                assertThat(r.getLevel()).isEqualTo(Level.WARNING);
                assertThat(r.getParameters()).containsExactly("quarkus.flyway.baseline-on-migrate",
                        "quarkus.flyway.baseline-version");
            }));

    @Test
    void warnsThatTheBaselineSettingsAreIgnored() {
    }
}
