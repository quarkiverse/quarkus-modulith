package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuditLog {

    @Inject
    Event<Object> events;

    public void publish(Object event) {
        events.fire(event);
    }
}
