package io.quarkiverse.modulith.deployment.fixtures.internalaccess.order;

public class OrderService {

    /**
     * The only reference to the other module is inside the method body, which Jandex alone would not see.
     */
    public int reserve() {
        return new io.quarkiverse.modulith.deployment.fixtures.internalaccess.inventory.internal.Stock().available();
    }
}
