package io.quarkiverse.modulith.it.violations.customer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.violations.customer.internal.CustomerRepository;

/**
 * The API other modules are meant to use.
 */
@ApplicationScoped
public class Customers {

    @Inject
    CustomerRepository repository;

    public String nameOf(String id) {
        return repository.findName(id);
    }
}
