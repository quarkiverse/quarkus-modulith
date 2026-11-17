package io.quarkiverse.modulith.it.flyway.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.flyway.catalog.Catalog;

/**
 * Owns the invoice lines table, which references the catalog's products, so the catalog migrates first.
 */
@ApplicationScoped
public class Billing {

    @Inject
    Catalog catalog;

    public String table() {
        return "invoice_lines references " + catalog.table();
    }
}
