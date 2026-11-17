package io.quarkiverse.modulith.deployment.fixtures.model.ddd.customers;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class Customer {
    @Identity
    Long id;
}
