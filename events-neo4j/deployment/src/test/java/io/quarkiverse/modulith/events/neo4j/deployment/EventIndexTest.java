package io.quarkiverse.modulith.events.neo4j.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;

import io.quarkus.test.QuarkusExtensionTest;

public class EventIndexTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Indexes.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.neo4j.devservices.image-name", "neo4j:5.26.33")
            .overrideConfigKey("quarkus.modulith.events.neo4j.event-index.enabled", "true");

    @Inject
    Driver driver;

    @Test
    void createsTheEventHashIndex() {
        // act
        List<String> indexes = Indexes.names(driver);

        // assert
        assertThat(indexes).contains("eventHashIndex");
    }

    @Test
    void createsTheUniqueConstraintsOnTheIdentifier() {
        // act
        List<String> constraints = Indexes.constraints(driver);

        // assert
        assertThat(constraints).contains("eventPublicationIdentifier", "eventPublicationArchiveIdentifier");
    }
}
