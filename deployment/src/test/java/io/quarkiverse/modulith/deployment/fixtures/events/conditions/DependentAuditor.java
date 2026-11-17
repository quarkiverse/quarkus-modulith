package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

import jakarta.enterprise.context.Dependent;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.ConditionalEventListener;

/**
 * A {@code @Dependent} bean gets a new instance for each decision, destroyed right after it.
 */
@Dependent
public class DependentAuditor implements ConditionalEventListener {

    @Override
    public boolean supports(Object event) {
        return ((Payment) event).amount() > 100;
    }

    @ApplicationModuleListener(async = false, id = "dependent")
    void dependent(Payment event) {
        ConditionListeners.HITS.add("dependent:" + event.reference());
    }
}
