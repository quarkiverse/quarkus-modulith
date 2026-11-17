package io.quarkiverse.modulith.events.jpa.deployment.fixtures.order;

public record OrderPlaced(String orderId, int amountInCents) {
}
