package io.quarkiverse.modulith.deployment.fixtures.badlistener.order;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class BadListener {

    @ApplicationModuleListener
    void on(String first, String second) {
    }
}
