package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class ShapeListeners {

    @ApplicationModuleListener(async = false, id = "labeled", condition = "#event.label == 'fragile'")
    void labeled(Labeled event) {
        ConditionListeners.HITS.add("labeled:" + event.label());
    }

    @ApplicationModuleListener(async = false, id = "signal", condition = "#event.flag and #event.name == 'x'"
            + " and #event.tags != null")
    void signal(Signal event) {
        ConditionListeners.HITS.add("signal:" + event.name());
    }
}
