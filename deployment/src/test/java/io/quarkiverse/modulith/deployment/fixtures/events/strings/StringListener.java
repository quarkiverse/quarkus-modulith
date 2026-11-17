package io.quarkiverse.modulith.deployment.fixtures.events.strings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * Listens to {@code String}, the payload type of the request context lifecycle events ArC fires. Synchronous, so a
 * delivery activates the request context on the firing thread, which fired the next lifecycle event and recursed until
 * the stack overflowed.
 */
public class StringListener {

    public static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(async = false)
    void on(String event) {
        RECEIVED.add(event);
    }
}
