package io.quarkiverse.modulith.it.moduletests.catalog;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Catalog {

    public boolean exists(String sku) {
        return sku.startsWith("sku-");
    }
}
