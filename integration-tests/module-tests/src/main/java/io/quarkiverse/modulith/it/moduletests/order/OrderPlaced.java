package io.quarkiverse.modulith.it.moduletests.order;

public record OrderPlaced(String orderId, String sku, int quantity) {
}
