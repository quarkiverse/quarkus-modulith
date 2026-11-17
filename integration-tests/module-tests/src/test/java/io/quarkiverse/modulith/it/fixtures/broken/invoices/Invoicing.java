package io.quarkiverse.modulith.it.fixtures.broken.invoices;

import io.quarkiverse.modulith.it.fixtures.broken.orders.internal.Ledger;

public class Invoicing {

    public void invoice(String order) {
        new Ledger().record(order);
    }
}
