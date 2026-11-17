package io.quarkiverse.modulith.events.fixtures.app.billing;

public record InvoicePaid(String number) implements Billable {
}
