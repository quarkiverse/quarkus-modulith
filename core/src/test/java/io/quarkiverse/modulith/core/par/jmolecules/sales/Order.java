package io.quarkiverse.modulith.core.par.jmolecules.sales;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class Order {
    @Identity
    Long id;

    public OrderCreated create() {
        return OrderCreated.of(this);
    }
}
