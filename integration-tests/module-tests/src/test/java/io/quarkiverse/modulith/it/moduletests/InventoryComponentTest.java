package io.quarkiverse.modulith.it.moduletests;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.it.moduletests.inventory.Inventory;
import io.quarkiverse.modulith.test.ApplicationModuleComponents;
import io.quarkus.test.component.QuarkusComponentTestExtension;

class InventoryComponentTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = ApplicationModuleComponents
            .of(InventoryComponentTest.class, "inventory").build();

    @Inject
    Inventory inventory;

    @Test
    void startsTheModuleSelectedByIdentifier() {
        // act
        int stock = inventory.stockOf("sku-unknown");

        // assert
        assertThat(stock).isEqualTo(100);
    }
}
