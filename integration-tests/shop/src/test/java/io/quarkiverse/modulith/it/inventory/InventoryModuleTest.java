package io.quarkiverse.modulith.it.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.it.notification.internal.NotificationListener;
import io.quarkiverse.modulith.it.order.OrderManagement;
import io.quarkiverse.modulith.it.order.events.OrderPlaced;
import io.quarkiverse.modulith.test.ApplicationModuleTestProfile;
import io.quarkiverse.modulith.test.BootstrapMode;
import io.quarkiverse.modulith.test.EnableScenarios;
import io.quarkiverse.modulith.test.PublishedEvents;
import io.quarkiverse.modulith.test.Scenario;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@EnableScenarios
@TestProfile(InventoryModuleTest.Profile.class)
class InventoryModuleTest {

    public static class Profile extends ApplicationModuleTestProfile {

        @Override
        protected BootstrapMode bootstrapMode() {
            return BootstrapMode.DIRECT_DEPENDENCIES;
        }
    }

    @Inject
    InventoryManagement inventory;

    @Inject
    OrderManagement orders;

    @Test
    void leavesOutModulesThatAreNotDependencies() {
        // act
        boolean available = Arc.container().instance(NotificationListener.class).isAvailable();

        // assert
        assertThat(available).isFalse();
    }

    @Test
    void reservesStockWhenAnOrderIsPlaced(Scenario scenario) {
        // arrange
        int before = inventory.stockOf("sku-1");

        // act
        int arrivedQuantity = scenario.publish(new OrderPlaced("o-module-test", "sku-1", 5))
                .andWaitForStateChange(() -> inventory.stockOf("sku-1"), stock -> stock == before - 5)
                .andExpect(OrderPlaced.class)
                .toArrive()
                .quantity();

        // assert
        assertThat(inventory.stockOf("sku-1")).isEqualTo(before - 5);
        assertThat(arrivedQuantity).isEqualTo(5);
    }

    @Test
    void ordersPublishTheEventTheInventoryConsumes(Scenario scenario) {
        // act
        OrderPlaced event = scenario.stimulate(() -> orders.place("sku-1", 2))
                .andWaitForEventOfType(OrderPlaced.class)
                .matching(e -> e.sku().equals("sku-1"))
                .toArrive();

        // assert
        assertThat(event.quantity()).isEqualTo(2);
        assertThat(PublishedEvents.current().ofType(OrderPlaced.class)).contains(event);
    }
}
