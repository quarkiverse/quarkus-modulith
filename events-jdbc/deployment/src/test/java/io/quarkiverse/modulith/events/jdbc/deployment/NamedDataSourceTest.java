package io.quarkiverse.modulith.events.jdbc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class NamedDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TableColumns.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:main;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.datasource.events.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.events.jdbc.url", "jdbc:h2:mem:events;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.modulith.events.jdbc.datasource", "events")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "update");

    @Inject
    AgroalDataSource main;

    @Inject
    @DataSource("events")
    AgroalDataSource events;

    @Test
    void createsNoTableWithoutSchemaInitialization() throws SQLException {
        // act
        int mainColumns = TableColumns.of(main, "EVENT_PUBLICATION").size();
        int eventsColumns = TableColumns.of(events, "EVENT_PUBLICATION").size();

        // assert
        assertThat(mainColumns).isZero();
        assertThat(eventsColumns).isZero();
    }
}
