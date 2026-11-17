package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class WildcardLocationsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(ShopApplication.archive())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", ShopApplication.BASE_PACKAGE)
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway.locations", "db/migration,db/*")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:wildcards;DB_CLOSE_DELAY=-1")
            .assertException(failure -> assertThat(failure).hasStackTraceContaining("The Flyway locations"
                    + " [classpath:db/*] contain wildcards, which would show the root migration the migrations of every"
                    + " module: list the locations without wildcards in quarkus.flyway.locations, or set"
                    + " quarkus.modulith.flyway.enabled=false"));

    @Test
    void failsTheStartup() {
    }
}
