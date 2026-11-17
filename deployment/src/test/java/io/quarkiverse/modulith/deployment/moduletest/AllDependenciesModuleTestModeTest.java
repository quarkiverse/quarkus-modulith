package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkiverse.modulith.deployment.fixtures.valid.catalog.internal.CatalogProductLookup;
import io.quarkiverse.modulith.deployment.fixtures.valid.inventory.InventoryManagement;
import io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderManagement;
import io.quarkiverse.modulith.deployment.fixtures.valid.shipping.ShippingService;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class AllDependenciesModuleTestModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.test.module", "shipping")
            .overrideConfigKey("quarkus.modulith.test.bootstrap-mode", "all-dependencies")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Test
    void startsTheModuleAndItsTransitiveDependencies() {
        // act
        boolean shipping = Arc.container().instance(ShippingService.class).isAvailable();
        boolean catalog = Arc.container().instance(CatalogProductLookup.class).isAvailable();
        boolean order = Arc.container().instance(OrderManagement.class).isAvailable();
        boolean inventory = Arc.container().instance(InventoryManagement.class).isAvailable();

        // assert
        assertThat(shipping).isTrue();
        assertThat(catalog).isTrue();
        assertThat(order).isTrue();
        assertThat(inventory).isFalse();
    }
}
