package io.quarkiverse.modulith.events.jms.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jms.deployment.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.jms.deployment.fixtures.support.FakeConnectionFactory;
import io.quarkiverse.modulith.events.jms.deployment.fixtures.support.SecondConnectionFactory;
import io.quarkus.test.QuarkusExtensionTest;

class AmbiguousConnectionFactoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addClasses(OrderPlaced.class, FakeConnectionFactory.class,
                    SecondConnectionFactory.class))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.events.jms.deployment.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(failure -> assertThat(failure)
                    .hasStackTraceContaining("Externalizing events to JMS needs one @Default"
                            + " jakarta.jms.ConnectionFactory bean, but found"
                            + " [io.quarkiverse.modulith.events.jms.deployment.fixtures.support.FakeConnectionFactory,"
                            + " io.quarkiverse.modulith.events.jms.deployment.fixtures.support.SecondConnectionFactory]:"
                            + " give all but one of them a qualifier, or mark the one to use @Alternative @Priority(1)"));

    @Test
    void failsTheBuildNamingTheFactories() {
    }
}
