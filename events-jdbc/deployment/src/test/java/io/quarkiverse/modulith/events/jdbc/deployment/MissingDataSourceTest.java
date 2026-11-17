package io.quarkiverse.modulith.events.jdbc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class MissingDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:missing")
            .overrideConfigKey("quarkus.modulith.events.jdbc.datasource", "outbox")
            .assertException(thrown -> assertThat(thrown).isInstanceOf(DeploymentException.class).cause()
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessage("The JDBC event publication registry uses the datasource 'outbox', which is not "
                            + "configured. Configure it with a db-kind, or point quarkus.modulith.events.jdbc.datasource "
                            + "to a configured datasource."));

    @Test
    void failsTheBuild() {
    }
}
