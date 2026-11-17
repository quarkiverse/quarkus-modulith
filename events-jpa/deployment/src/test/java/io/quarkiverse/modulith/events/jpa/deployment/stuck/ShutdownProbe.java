package io.quarkiverse.modulith.events.jpa.deployment.stuck;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkus.runtime.ShutdownEvent;

/**
 * Looks at the outcome after the dispatcher drained (priority 100) and the registry reported (200), and leaves it in a
 * system property, the one place the test can still read once the application and its class loader are gone.
 */
@Singleton
public class ShutdownProbe {

    public static final String PROPERTY = "modulith.test.shutdown-probe";

    @Inject
    IncompleteEventPublications incomplete;

    void onShutdown(@Observes @Priority(300) ShutdownEvent event) {
        System.setProperty(PROPERTY,
                "finished=" + StuckListener.FINISHED.size() + ",incomplete=" + incomplete.findIncompletePublications().size());
    }
}
