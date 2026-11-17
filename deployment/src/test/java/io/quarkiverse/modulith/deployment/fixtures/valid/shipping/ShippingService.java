package io.quarkiverse.modulith.deployment.fixtures.valid.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.deployment.fixtures.valid.catalog.spi.ProductLookup;
import io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderCompleted;

@ApplicationScoped
public class ShippingService {

    @Inject
    ProductLookup products;

    public String label(OrderCompleted order) {
        return order.orderId() + ": " + products.name("sku-1");
    }
}
