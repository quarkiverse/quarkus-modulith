package io.quarkiverse.modulith.deployment.docs.fixtures.app.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.modulith.deployment.docs.fixtures.app.catalog.Catalog;

@ApplicationScoped
public class Timeouts {

    final int retry;
    final Catalog catalog;

    @Inject
    Timeouts(@ConfigProperty(name = "shop.retry", defaultValue = "3") int retry, Catalog catalog) {
        this.retry = retry;
        this.catalog = catalog;
    }
}
