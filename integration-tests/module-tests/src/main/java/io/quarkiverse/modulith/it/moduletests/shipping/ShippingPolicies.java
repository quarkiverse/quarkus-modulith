package io.quarkiverse.modulith.it.moduletests.shipping;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Declares only a producer, so it is a bean without a bean defining annotation.
 */
public class ShippingPolicies {

    @Produces
    @Singleton
    ShippingPolicy policy() {
        return new ShippingPolicy(10);
    }
}
