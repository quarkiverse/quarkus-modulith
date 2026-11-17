package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class StaticListener {

    @ApplicationModuleListener
    static void on(String event) {
    }
}
