package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.ConditionalEventListener;

/**
 * Decides itself which payments it handles, and one of its listeners adds a condition.
 */
public class ConditionalAuditor implements ConditionalEventListener {

    @Override
    public boolean supports(Object event) {
        return ((Payment) event).amount() % 2 == 0;
    }

    @ApplicationModuleListener(async = false, id = "even")
    void even(Payment event) {
        ConditionListeners.HITS.add("even:" + event.reference());
    }

    @ApplicationModuleListener(async = false, id = "even-urgent", condition = "#event.urgent")
    void evenUrgent(Payment event) {
        ConditionListeners.HITS.add("even-urgent:" + event.reference());
    }
}
