package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.requestcontext.RequestMarker;
import io.quarkiverse.modulith.deployment.fixtures.events.requestcontext.Visit;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;

public class VirtualThreadListenerRequestContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Visit.class.getPackage())
                    .addClass(RequestContextPublisher.class))
            .overrideConfigKey("quarkus.modulith.base-package", Visit.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.listeners.executor", "virtual-threads");

    @Inject
    Event<Visit> events;

    @Inject
    RequestMarker marker;

    @Inject
    Vertx vertx;

    @Test
    void runsAsynchronousListenersInAFreshRequestContext() throws Exception {
        // arrange
        String id = "v-1";

        // act
        List<String> seen = RequestContextPublisher.publishFromRequest(vertx, events, marker, id);

        // assert
        assertThat(seen).containsExactly(id + ":" + RequestMarker.FRESH);
    }
}
