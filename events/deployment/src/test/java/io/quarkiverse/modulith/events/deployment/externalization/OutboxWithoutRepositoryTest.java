package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderPlaced;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;

class OutboxWithoutRepositoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, OrderPlaced.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.events.externalization.mode", "outbox")
            .assertException(failure -> assertThat(failure).rootCause().hasMessageContaining(
                    "The outbox externalization mode records events as event publications and needs an"
                            + " EventPublicationRepository bean: add an event publication store such as"
                            + " quarkus-modulith-events-jpa, or quarkus-modulith-events-jobrunr to use JobRunr as the"
                            + " outbox, or set quarkus.modulith.events.externalization.mode=module-listener"));

    @Test
    void failsTheBuild() {
    }
}
