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

/**
 * Runs with {@code quarkus-smallrye-context-propagation} installed (it comes with {@code quarkus-narayana-jta}), so the
 * unqualified {@code ExecutorService} behind the worker executor is shown not to carry the request context.
 */
public class WorkerListenerRequestContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Visit.class.getPackage())
                    .addClass(RequestContextPublisher.class))
            .overrideConfigKey("quarkus.modulith.base-package", Visit.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

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
