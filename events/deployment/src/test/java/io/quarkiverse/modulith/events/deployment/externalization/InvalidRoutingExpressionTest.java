package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.invalid.BadEvent;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.RecordingTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.support.SentMessage;
import io.quarkus.test.QuarkusExtensionTest;

class InvalidRoutingExpressionTest {

    private static final String INVALID = "io.quarkiverse.modulith.events.deployment.externalization.fixtures.invalid";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(archive -> archive.addPackages(true, BadEvent.class.getPackage().getName())
                    .addClasses(RecordingTransport.class, SentMessage.class))
            .overrideConfigKey("quarkus.modulith.base-package", INVALID)
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(failure -> assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Invalid routing expressions:\n"
                            + "@Externalized on " + INVALID + ".BadEvent: Invalid routing expression '{event.missing}' for "
                            + INVALID + ".BadEvent: " + INVALID + ".BadEvent has no accessible property missing\n"
                            + "@Externalized on " + INVALID + ".WorseEvent: Unsupported routing expression"
                            + " '#{T(java.util.UUID).randomUUID()}': an expression starts with #this, #root or @bean; "
                            + RoutingExpressionParser.SUPPORTED_FORMS));

    @Test
    void failsTheBuildWithEveryProblem() {
    }
}
