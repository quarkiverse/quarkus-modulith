package io.quarkiverse.modulith.it.violations;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Records the modules whose initializers ran, in order.
 */
@ApplicationScoped
public class Initializations {

    private final List<String> modules = new CopyOnWriteArrayList<>();

    public void ran(String module) {
        modules.add(module);
    }

    public List<String> modules() {
        return List.copyOf(modules);
    }
}
