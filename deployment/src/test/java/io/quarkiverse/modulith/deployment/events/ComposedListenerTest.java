package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.composed.ComposedListeners;
import io.quarkiverse.modulith.deployment.fixtures.events.composed.Shipped;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class ComposedListenerTest {

    static final String PACKAGE = "io.quarkiverse.modulith.deployment.fixtures.events.composed.";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Shipped.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Shipped.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Event<Shipped> events;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void honorsComposedAnnotationsAtAnyDepthWithSpringListenerIds() {
        // arrange
        ComposedListeners.HITS.clear();
        String thread = Thread.currentThread().getName();

        // act
        events.fire(new Shipped("s-1"));

        // assert
        assertThat(ComposedListeners.HITS).containsExactlyInAnyOrder("direct:s-1:" + thread, "nested:s-1:" + thread,
                "twice:s-1:" + thread);
        assertThat(dispatcher.listenerIds()).containsExactlyInAnyOrder(
                PACKAGE + "ComposedListeners.direct(" + PACKAGE + "Shipped)",
                PACKAGE + "ComposedListeners.nested(" + PACKAGE + "Shipped)",
                PACKAGE + "ComposedListeners.twice(" + PACKAGE + "Shipped)");
    }
}
