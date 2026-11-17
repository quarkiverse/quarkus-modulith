package io.quarkiverse.modulith.it.violations.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.violations.customer.internal.CustomerRepository;

/**
 * Breaks the rules: goes straight to the customer module's repository instead of its {@code Customers} API.
 */
@ApplicationScoped
public class InvoiceService {

    @Inject
    CustomerRepository customers;

    public String invoiceFor(String customerId) {
        return "Invoice for " + customers.findName(customerId);
    }
}
