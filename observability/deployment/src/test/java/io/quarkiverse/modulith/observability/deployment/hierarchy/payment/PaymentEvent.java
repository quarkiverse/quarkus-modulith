package io.quarkiverse.modulith.observability.deployment.hierarchy.payment;

public sealed interface PaymentEvent permits PaymentReceived, PaymentRefunded {
}
