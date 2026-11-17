package io.quarkiverse.modulith.it.order.events;

public record OrderPlaced(String orderId, String sku, int quantity) {
}
