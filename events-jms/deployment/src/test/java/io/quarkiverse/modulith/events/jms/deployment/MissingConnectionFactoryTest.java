package io.quarkiverse.modulith.events.jms.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jms.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkus.test.QuarkusExtensionTest;

class MissingConnectionFactoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addClasses(OrderPlaced.class))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.events.jms.deployment.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(failure -> assertThat(failure)
                    .hasStackTraceContaining("Externalizing events to JMS needs a jakarta.jms.ConnectionFactory bean:"
                            + " add a JMS client extension such as io.quarkiverse.artemis:quarkus-artemis-jms"));

    @Test
    void failsTheBuildWithTheFix() {
    }
}
