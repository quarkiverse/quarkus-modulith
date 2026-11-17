package io.quarkiverse.modulith.deployment.fixtures.events.nomodel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The events the listener of the default package received.
 */
public final class Deliveries {

    public static final List<Object> RECEIVED = new CopyOnWriteArrayList<>();

    private Deliveries() {
    }
}
