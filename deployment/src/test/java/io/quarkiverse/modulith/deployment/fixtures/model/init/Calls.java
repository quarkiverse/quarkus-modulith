package io.quarkiverse.modulith.deployment.fixtures.model.init;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records the order in which the initializers run.
 */
public final class Calls {

    public static final List<String> CALLS = new CopyOnWriteArrayList<>();

    private Calls() {
    }
}
