package io.quarkiverse.modulith.deployment.docs.fixtures.app.catalog;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Catalog {

    final String name;

    Catalog() {
        this("default");
    }

    Catalog(String name) {
        this.name = name;
    }
}
