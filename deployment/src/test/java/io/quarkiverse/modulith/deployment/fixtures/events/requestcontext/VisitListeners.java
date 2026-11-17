package io.quarkiverse.modulith.deployment.fixtures.events.requestcontext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * Records the request-scoped mark an asynchronous listener sees.
 */
public class VisitListeners {

    public static final List<String> SEEN = new CopyOnWriteArrayList<>();

    @Inject
    RequestMarker marker;

    @ApplicationModuleListener(id = "visit")
    void on(Visit event) {
        SEEN.add(event.id() + ":" + marker.mark());
    }
}
