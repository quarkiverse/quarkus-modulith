package io.quarkiverse.modulith.docs.fixtures.config.billing;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class InvoiceSender {

    @ConfigProperty(name = "billing.sender")
    String sender;

    @ConfigProperty
    long timeout;

    String unconfigured;

    public InvoiceSender(@ConfigProperty(name = "billing.retries", defaultValue = "2") int retries, String plain) {
        this.unconfigured = plain + retries;
    }
}
