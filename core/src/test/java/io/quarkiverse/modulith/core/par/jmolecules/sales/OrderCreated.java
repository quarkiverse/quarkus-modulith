package io.quarkiverse.modulith.core.par.jmolecules.sales;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public class OrderCreated {
    public static OrderCreated of(Order order) {
        return new OrderCreated();
    }
}
