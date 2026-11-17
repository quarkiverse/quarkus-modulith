package io.quarkiverse.modulith.events.jpa.deployment.externalized;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Publishes without a transaction, as a plain REST resource or a scheduled method does.
 */
@ApplicationScoped
public class ShipmentService {

    @Inject
    Event<ShipmentDispatched> dispatched;

    public void dispatch(String id) {
        dispatched.fire(new ShipmentDispatched(id));
    }
}
