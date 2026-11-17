package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class PrivateListener {

    @ApplicationModuleListener
    private void on(String event) {
    }
}
