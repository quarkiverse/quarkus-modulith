package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.prod.RedeliveryClient;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusProdModeTest;

class NoListenersProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest prod = new QuarkusProdModeTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class, RedeliveryClient.class))
            .setApplicationName("externalization-without-listeners")
            .setApplicationVersion("1.0")
            .setRun(true)
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Test
    void registersARedeliveryForExternalizationPublications() {
        // act
        String output = prod.getStartupConsoleOutput();

        // assert
        assertThat(output).contains("Redelivery of an unknown listener: false");
    }
}
