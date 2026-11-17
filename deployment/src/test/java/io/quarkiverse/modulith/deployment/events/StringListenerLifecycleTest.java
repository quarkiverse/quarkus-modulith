package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.strings.StringListener;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A synchronous {@code String} listener used to receive the {@code String} payload of the request context lifecycle
 * events. Activating the request context for that delivery fired the next lifecycle event, until the stack
 * overflowed. Activating the request context must now leave the listener alone.
 */
public class StringListenerLifecycleTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(StringListener.class))
            .overrideConfigKey("quarkus.modulith.base-package", StringListener.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Event<String> events;

    @BeforeEach
    void reset() {
        StringListener.RECEIVED.clear();
    }

    @Test
    void ignoresRequestContextLifecycleEventsAndDeliversFiredStrings() {
        // arrange
        ManagedContext requestContext = Arc.container().requestContext();

        // act
        requestContext.activate();
        requestContext.terminate();
        events.fire("hello");

        // assert
        assertThat(StringListener.RECEIVED).containsExactly("hello");
    }
}
