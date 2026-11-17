package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

import io.quarkiverse.modulith.ApplicationModuleListener;

/**
 * Inherits {@code ConditionalEventListener} from its superclass.
 */
public class SubAuditor extends BaseAuditor {

    @ApplicationModuleListener(async = false, id = "sub")
    void sub(Payment event) {
        ConditionListeners.HITS.add("sub:" + event.reference());
    }
}
