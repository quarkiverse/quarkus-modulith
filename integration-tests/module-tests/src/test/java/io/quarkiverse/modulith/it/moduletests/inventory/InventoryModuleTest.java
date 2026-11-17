package io.quarkiverse.modulith.it.moduletests.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.it.moduletests.audit.AuditLog;
import io.quarkiverse.modulith.it.moduletests.catalog.Catalog;
import io.quarkiverse.modulith.it.moduletests.order.OrderPlaced;
import io.quarkiverse.modulith.it.moduletests.order.Orders;
import io.quarkiverse.modulith.it.moduletests.shared.Sequence;
import io.quarkiverse.modulith.it.moduletests.shipping.Shipping;
import io.quarkiverse.modulith.test.ApplicationModuleTest;
import io.quarkiverse.modulith.test.ApplicationModuleTestProfile;
import io.quarkiverse.modulith.test.BootstrapMode;
import io.quarkiverse.modulith.test.ModuleTestExecution;
import io.quarkiverse.modulith.test.Scenario;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.TestProfile;

@ApplicationModuleTest
@TestProfile(InventoryModuleTest.Profile.class)
class InventoryModuleTest {

    public static class Profile extends ApplicationModuleTestProfile {
    }

    @Inject
    Inventory inventory;

    @ParameterizedTest
    @ValueSource(classes = { Inventory.class, Sequence.class })
    void startsTheModuleAndTheSharedModule(Class<?> type) {
        // act
        boolean available = Arc.container().instance(type).isAvailable();

        // assert
        assertThat(available).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = { Orders.class, Catalog.class, Shipping.class, AuditLog.class })
    void leavesOutTheOtherModules(Class<?> type) {
        // act
        boolean available = Arc.container().instance(type).isAvailable();

        // assert
        assertThat(available).isFalse();
    }

    @Test
    void reservesStockWhenAnOrderIsPlaced(Scenario scenario) {
        // arrange
        int before = inventory.stockOf("sku-standalone");

        // act
        int after = scenario.publish(new OrderPlaced("o-1", "sku-standalone", 5))
                .andWaitAtMost(Duration.ofSeconds(5))
                .andWaitForStateChange(() -> inventory.stockOf("sku-standalone"), stock -> stock == before - 5)
                .andExpect(OrderPlaced.class)
                .toArrive()
                .quantity();

        // assert
        assertThat(after).isEqualTo(5);
    }

    @Test
    void describesTheExecution(ModuleTestExecution execution) {
        // act
        String module = execution.getModule().getName();

        // assert
        assertThat(module).isEqualTo("inventory");
        assertThat(execution.getBootstrapMode()).isEqualTo(BootstrapMode.STANDALONE);
        assertThat(execution.getDependencies()).isEmpty();
        assertThat(execution.getSharedModules()).extracting(ApplicationModule::getName).containsExactly("shared");
        assertThat(execution).extracting(ApplicationModule::getName).containsExactly("inventory", "shared");
    }
}
