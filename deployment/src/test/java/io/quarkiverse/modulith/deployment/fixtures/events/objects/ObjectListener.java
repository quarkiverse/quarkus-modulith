package io.quarkiverse.modulith.deployment.fixtures.events.objects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * Listens to every event. ArC fires the context lifecycle events with the declared type {@code Object}, so this
 * listener's observer receives them, and a synchronous delivery activating the request context would fire the next.
 */
public class ObjectListener {

    public static final List<Object> RECEIVED = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(async = false)
    void on(Object event) {
        RECEIVED.add(event);
    }
}
