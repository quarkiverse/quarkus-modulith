package io.quarkiverse.modulith.deployment.fixtures.model.ddd.orders;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import io.quarkiverse.modulith.deployment.fixtures.model.ddd.customers.Customer;

@AggregateRoot
public class Order {
    @Identity
    Long id;

    Customer customer;
}
