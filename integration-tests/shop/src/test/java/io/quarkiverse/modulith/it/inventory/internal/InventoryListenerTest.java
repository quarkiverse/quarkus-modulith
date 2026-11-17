package io.quarkiverse.modulith.it.inventory.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.it.order.events.OrderPlaced;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class InventoryListenerTest {

    private static final String UNKNOWN_SKU = "sku-unknown";

    @Inject
    InventoryListener listener;

    @Test
    void rejectsAnOrderForAnUnknownSku() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-unknown-sku", UNKNOWN_SKU, 1);

        // act & assert
        assertThatThrownBy(() -> listener.on(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown sku " + UNKNOWN_SKU);
    }
}
