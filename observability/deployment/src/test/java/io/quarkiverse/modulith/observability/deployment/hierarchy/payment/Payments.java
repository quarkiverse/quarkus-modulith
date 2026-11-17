package io.quarkiverse.modulith.observability.deployment.hierarchy.payment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class Payments {

    @Inject
    Event<PaymentReceived> received;

    @Inject
    Event<PaymentRefunded> refunded;

    public void receive(String id) {
        received.fire(new PaymentReceived(id));
    }

    public void refund(String id) {
        refunded.fire(new PaymentRefunded(id));
    }
}
