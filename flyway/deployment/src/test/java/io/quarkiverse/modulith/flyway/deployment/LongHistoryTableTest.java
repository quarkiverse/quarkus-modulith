package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The history table name has 55 characters, so only the table of {@code shipping}, the longest module identifier,
 * exceeds 63.
 */
class LongHistoryTableTest {

    private static final String TABLE = "shop_schema_history_of_the_flyway_migrations_of_the_app";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(ShopApplication.archive())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", ShopApplication.BASE_PACKAGE)
            .overrideConfigKey("quarkus.modulith.flyway.enabled", "true")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true")
            .overrideConfigKey("quarkus.flyway.table", TABLE)
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:long-tables;DB_CLOSE_DELAY=-1")
            .assertException(failure -> assertThat(failure).hasStackTraceContaining("The Flyway history tables ["
                    + TABLE + "_shipping] have more than 63 characters: shorten"
                    + " quarkus.flyway.table or the module identifiers"));

    @Test
    void failsTheStartup() {
    }
}
