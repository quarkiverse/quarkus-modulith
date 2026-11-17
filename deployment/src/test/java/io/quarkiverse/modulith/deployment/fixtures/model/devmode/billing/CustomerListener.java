package io.quarkiverse.modulith.deployment.fixtures.model.devmode.billing;

import jakarta.enterprise.event.Observes;

import io.quarkiverse.modulith.deployment.fixtures.model.devmode.customer.Customer;

public class CustomerListener {
    void on(@Observes Customer customer) {
    }
}
