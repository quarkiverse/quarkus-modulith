package io.quarkiverse.modulith.deployment.fixtures.events.conditions;

public record Payment(String reference, long amount, String currency, Customer customer, boolean urgent,
        PaymentState state) implements Auditable {
}
