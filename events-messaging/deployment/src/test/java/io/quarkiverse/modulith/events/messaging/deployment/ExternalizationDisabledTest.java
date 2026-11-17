package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.support.ShopApplication;
import io.quarkiverse.modulith.events.messaging.impl.MessagingChannels;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

class ExternalizationDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = ShopApplication.create()
            .overrideConfigKey("quarkus.modulith.events.externalization.enabled", "false")
            .overrideConfigKey("quarkus.modulith.events.messaging.channels", "audit");

    @Test
    void registersNeitherChannelsNorTransport() {
        // act
        boolean noChannels = Arc.container().select(MessagingChannels.class).isUnsatisfied();
        boolean noTransport = Arc.container().select(EventExternalizationTransport.class).isUnsatisfied();

        // assert
        assertThat(noChannels).isTrue();
        assertThat(noTransport).isTrue();
    }
}
