package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class BlankIdListener {

    @ApplicationModuleListener(id = " ", async = true)
    void on(Integer event) {
    }
}
