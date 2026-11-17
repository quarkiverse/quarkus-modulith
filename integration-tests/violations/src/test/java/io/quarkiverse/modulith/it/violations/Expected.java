package io.quarkiverse.modulith.it.violations;

import java.util.List;

import io.quarkiverse.modulith.core.Violation;

/**
 * The violations of the billing module, with Spring Modulith's messages.
 */
final class Expected {

    static final String BASE = "io.quarkiverse.modulith.it.violations";
    static final String SERVICE = BASE + ".billing.InvoiceService";
    static final String REPOSITORY = BASE + ".customer.internal.CustomerRepository";
    static final String HEADER = "Module 'billing' depends on non-exposed type " + REPOSITORY + " within module 'customer'!"
            + System.lineSeparator();

    static final List<Violation> VIOLATIONS = List.of(
            violation("Field <" + SERVICE + ".customers> has type <" + REPOSITORY + "> in (InvoiceService.java:0)"),
            violation("InvoiceService declares injected field @Inject InvoiceService.customers in (InvoiceService.java:0)"),
            violation("Method <" + SERVICE + ".invoiceFor(java.lang.String)> calls method <" + REPOSITORY
                    + ".findName(java.lang.String)> in (InvoiceService.java:18)"));

    private Expected() {
    }

    private static Violation violation(String description) {
        return new Violation(Violation.Kind.NON_EXPOSED_TYPE, "billing", SERVICE, REPOSITORY, HEADER + description);
    }
}
