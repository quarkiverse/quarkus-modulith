package io.quarkiverse.modulith.it.moduletests.order;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.it.moduletests.audit.AuditLog;
import io.quarkiverse.modulith.it.moduletests.catalog.Catalog;
import io.quarkiverse.modulith.it.moduletests.inventory.Inventory;
import io.quarkiverse.modulith.it.moduletests.shipping.Shipping;
import io.quarkiverse.modulith.test.ApplicationModuleTest;
import io.quarkiverse.modulith.test.ApplicationModuleTestProfile;
import io.quarkiverse.modulith.test.BootstrapMode;
import io.quarkiverse.modulith.test.ModuleTestExecution;
import io.quarkiverse.modulith.test.PublishedEvents;
import io.quarkiverse.modulith.test.Scenario;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.TestProfile;

@ApplicationModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
@TestProfile(OrderModuleTest.Profile.class)
class OrderModuleTest {

    public static class Profile extends ApplicationModuleTestProfile {
    }

    @Inject
    Orders orders;

    @Test
    void startsTheDirectDependencies(ModuleTestExecution execution) {
        // act
        boolean catalog = Arc.container().instance(Catalog.class).isAvailable();

        // assert
        assertThat(catalog).isTrue();
        assertThat(Arc.container().instance(Inventory.class).isAvailable()).isFalse();
        assertThat(Arc.container().instance(Shipping.class).isAvailable()).isFalse();
        assertThat(Arc.container().instance(AuditLog.class).isAvailable()).isFalse();
        assertThat(execution.getBootstrapMode()).isEqualTo(BootstrapMode.DIRECT_DEPENDENCIES);
        assertThat(execution.getDependencies()).extracting(ApplicationModule::getName).containsExactlyInAnyOrder("catalog",
                "shared");
        assertThat(execution.isIncludedInExecution("inventory")).isFalse();
        assertThat(execution.includes(Inventory.class.getName())).isFalse();
        assertThat(execution.includes(Catalog.class.getName())).isTrue();
    }

    @Test
    void publishesOrderPlaced(Scenario scenario, PublishedEvents events) {
        // act
        String orderId = scenario.stimulate(() -> orders.place("sku-direct", 2))
                .andWaitForEventOfType(OrderPlaced.class)
                .matchingMappedValue(OrderPlaced::sku, "sku-direct")
                .toArrive()
                .orderId();

        // assert
        assertThat(events.ofType(OrderPlaced.class)).containsExactly(new OrderPlaced(orderId, "sku-direct", 2));
    }
}
