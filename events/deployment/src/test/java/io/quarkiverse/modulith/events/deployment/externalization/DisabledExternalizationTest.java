package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkiverse.modulith.events.externalization.EventExternalizer;
import io.quarkus.test.QuarkusExtensionTest;

class DisabledExternalizationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class))
            .overrideConfigKey("quarkus.modulith.events.externalization.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    Instance<EventExternalizer> externalizer;

    @Inject
    Instance<EventExternalizationConfiguration> configuration;

    @Test
    void registersNothingWhenDisabled() {
        // act
        boolean noExternalizer = externalizer.isUnsatisfied();
        boolean noConfiguration = configuration.isUnsatisfied();

        // assert
        assertThat(noExternalizer).isTrue();
        assertThat(noConfiguration).isTrue();
    }
}
