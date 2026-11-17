package io.quarkiverse.modulith.events.neo4j.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;

import io.quarkiverse.modulith.events.neo4j.runtime.Neo4jStore;
import io.quarkus.test.QuarkusExtensionTest;

public class NoEventIndexTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Indexes.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.neo4j.devservices.image-name", "neo4j:5.26.33");

    @Inject
    Driver driver;

    @Inject
    Neo4jStore store;

    @Test
    void createsNoIndex() {
        // arrange
        driver.executableQuery("DROP INDEX eventHashIndex IF EXISTS").execute();
        driver.executableQuery("DROP CONSTRAINT eventPublicationIdentifier IF EXISTS").execute();
        driver.executableQuery("DROP CONSTRAINT eventPublicationArchiveIdentifier IF EXISTS").execute();

        // act
        List<String> indexes = Indexes.names(driver);
        List<String> constraints = Indexes.constraints(driver);

        // assert
        assertThat(indexes).doesNotContain("eventHashIndex");
        assertThat(constraints).doesNotContain("eventPublicationIdentifier", "eventPublicationArchiveIdentifier");
        assertThat(store.sessionConfig().database()).isEmpty();
    }
}
