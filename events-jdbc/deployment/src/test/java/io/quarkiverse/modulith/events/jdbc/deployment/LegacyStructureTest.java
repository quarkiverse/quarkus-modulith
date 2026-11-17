package io.quarkiverse.modulith.events.jdbc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.jdbc.runtime.LegacyJdbcEventPublicationRepository;
import io.quarkus.test.QuarkusExtensionTest;

public class LegacyStructureTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TableColumns.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:legacy;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.modulith.events.jdbc.schema-initialization.enabled", "true")
            .overrideConfigKey("quarkus.modulith.events.jdbc.use-legacy-structure", "true");

    @Inject
    EventPublicationRepository repository;

    @Inject
    AgroalDataSource dataSource;

    @Test
    void usesTheLegacyRepositoryAndTable() throws SQLException {
        // act
        List<String> columns = TableColumns.of(dataSource, "EVENT_PUBLICATION");

        // assert
        assertThat(repository).isInstanceOf(LegacyJdbcEventPublicationRepository.class);
        assertThat(columns).containsExactlyInAnyOrder("ID", "COMPLETION_DATE", "EVENT_TYPE", "LISTENER_ID",
                "PUBLICATION_DATE", "SERIALIZED_EVENT");
    }
}
