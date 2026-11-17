package io.quarkiverse.modulith.test.slice.billing;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class ListenerOnly {

    @ApplicationModuleListener
    void on(Integer event) {
    }
}
