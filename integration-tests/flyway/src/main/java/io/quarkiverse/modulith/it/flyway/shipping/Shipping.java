package io.quarkiverse.modulith.it.flyway.shipping;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Owns the shipments table, independent of the other modules.
 */
@ApplicationScoped
public class Shipping {

    public String table() {
        return "shipments";
    }
}
