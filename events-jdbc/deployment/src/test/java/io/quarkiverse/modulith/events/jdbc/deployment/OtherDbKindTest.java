package io.quarkiverse.modulith.events.jdbc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jdbc.runtime.DatabaseType;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcDatabase;
import io.quarkiverse.modulith.events.jdbc.runtime.JdbcStore;
import io.quarkus.test.QuarkusExtensionTest;

public class OtherDbKindTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "other")
            .overrideConfigKey("quarkus.datasource.jdbc.driver", "org.h2.Driver")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:other");

    @Inject
    JdbcDatabase database;

    @Inject
    JdbcStore store;

    @Test
    void detectsTheDatabaseAtStartup() {
        // act
        DatabaseType detected = store.settings().databaseType();

        // assert
        assertThat(database.type()).isEmpty();
        assertThat(detected).isEqualTo(DatabaseType.H2);
    }
}
