package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkiverse.modulith.deployment.fixtures.valid.catalog.internal.CatalogProductLookup;
import io.quarkiverse.modulith.deployment.fixtures.valid.inventory.InventoryManagement;
import io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderManagement;
import io.quarkiverse.modulith.deployment.fixtures.valid.shipping.ShippingService;
import io.quarkiverse.modulith.deployment.moduletest.SetupResource;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class StandaloneModuleTestModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage())
                    .addClass(SetupResource.class))
            .overrideConfigKey("quarkus.modulith.test.module", "inventory")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Test
    void startsOnlyTheModuleUnderTest() {
        // act
        boolean inventory = Arc.container().instance(InventoryManagement.class).isAvailable();

        // assert
        assertThat(inventory).isTrue();
        assertThat(SetupResource.read()).isEqualTo("module=inventory\nbootstrap-mode=STANDALONE\ndependencies=\n"
                + "extra-includes=\nshared-modules=shared\n");
        assertThat(Arc.container().instance(OrderManagement.class).isAvailable()).isFalse();
        assertThat(Arc.container().instance(ShippingService.class).isAvailable()).isFalse();
        assertThat(Arc.container().instance(CatalogProductLookup.class).isAvailable()).isFalse();
    }
}
