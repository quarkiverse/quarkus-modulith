package io.quarkiverse.modulith.deployment.fixtures.events.invalid.order;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * Excluded from bean discovery by the test, so its listener is not on a bean.
 */
public class NotABean {

    @ApplicationModuleListener
    void on(Short event) {
    }
}
