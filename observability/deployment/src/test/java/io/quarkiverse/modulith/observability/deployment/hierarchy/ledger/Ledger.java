package io.quarkiverse.modulith.observability.deployment.hierarchy.ledger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.PaymentEvent;

@ApplicationScoped
public class Ledger {

    private final List<PaymentEvent> entries = new CopyOnWriteArrayList<>();

    void on(@Observes PaymentEvent event) {
        entries.add(event);
    }

    public List<PaymentEvent> entries() {
        return List.copyOf(entries);
    }
}
