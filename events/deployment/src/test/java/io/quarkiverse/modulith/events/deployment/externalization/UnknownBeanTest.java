package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.unnamed.LooseEvent;
import io.quarkus.test.QuarkusExtensionTest;

class UnknownBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, LooseEvent.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class))
            .overrideConfigKey("quarkus.modulith.base-package",
                    "io.quarkiverse.modulith.events.deployment.externalization.fixtures.unnamed")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(failure -> assertThat(failure).rootCause().hasMessageContaining(
                    "The routing expressions of @Externalized call the bean loose, which is not a CDI bean: give the"
                            + " class annotated with @Named(\"loose\") a scope such as @ApplicationScoped"));

    @Test
    void failsTheBuild() {
    }
}
