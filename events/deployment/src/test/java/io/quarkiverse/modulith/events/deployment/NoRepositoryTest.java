package io.quarkiverse.modulith.events.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.registry.DefaultEventPublicationRegistry;
import io.quarkiverse.modulith.events.registry.EventPublicationJobs;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class NoRepositoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Test
    void registersNoRegistry() {
        // arrange
        var container = Arc.container();

        // act
        boolean registry = container.instance(DefaultEventPublicationRegistry.class).isAvailable();
        boolean jobs = container.instance(EventPublicationJobs.class).isAvailable();

        // assert
        assertThat(registry).isFalse();
        assertThat(jobs).isFalse();
    }
}
