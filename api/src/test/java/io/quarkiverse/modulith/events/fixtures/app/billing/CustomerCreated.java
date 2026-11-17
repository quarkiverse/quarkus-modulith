package io.quarkiverse.modulith.events.fixtures.app.billing;

@DomainEvent
public record CustomerCreated(String lastname) {
}
