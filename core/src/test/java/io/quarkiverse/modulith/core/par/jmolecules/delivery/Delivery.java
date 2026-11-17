package io.quarkiverse.modulith.core.par.jmolecules.delivery;

import org.jmolecules.event.annotation.DomainEventHandler;

import io.quarkiverse.modulith.core.par.jmolecules.sales.OrderCreated;

public class Delivery {
    @DomainEventHandler
    void on(OrderCreated event) {
    }
}
