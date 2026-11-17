package io.quarkiverse.modulith.flyway.deployment.fixtures.shop.billing;

import io.quarkiverse.modulith.flyway.deployment.fixtures.shop.shipping.Shipments;

public class Invoices {

    final Shipments shipments = new Shipments();

    public String bill(String parcel) {
        return shipments.track(parcel);
    }
}
