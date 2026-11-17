package io.quarkiverse.modulith.it.violations.customer.internal;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerRepository {

    public String findName(String id) {
        return "Customer " + id;
    }
}
