package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class DuplicateListener {

    @ApplicationModuleListener(id = "dup")
    void first(String event) {
    }

    @ApplicationModuleListener(id = "dup")
    void second(Long event) {
    }
}
