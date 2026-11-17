package io.quarkiverse.modulith.it.flyway.catalog;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Owns the products table.
 */
@ApplicationScoped
public class Catalog {

    public String table() {
        return "products";
    }
}
