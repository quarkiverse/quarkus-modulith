package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

import io.quarkiverse.modulith.events.ConditionalEventListener;

public abstract class BaseAuditor implements ConditionalEventListener {

    @Override
    public boolean supports(Object event) {
        return ((Payment) event).urgent();
    }
}
