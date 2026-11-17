package io.quarkiverse.modulith.core.fixtures.shop.order;

public record OrderPlaced(long id) {
    public static OrderPlaced of(long id) {
        return new OrderPlaced(id);
    }

    public static String describe(long id) {
        return "order " + id;
    }
}
