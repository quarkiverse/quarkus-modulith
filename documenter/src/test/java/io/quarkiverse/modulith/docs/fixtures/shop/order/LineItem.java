package io.quarkiverse.modulith.docs.fixtures.shop.order;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record LineItem(String product, int quantity) {
}
