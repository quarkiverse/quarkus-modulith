package io.quarkiverse.modulith.core.fixtures.diagram.alerts;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.core.fixtures.diagram.billing.InvoicePaid;

@ApplicationScoped
public class PaymentAlerts {

    @ApplicationModuleListener
    void on(InvoicePaid event) {
    }
}
