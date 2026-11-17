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

public class DirectDependenciesModuleTestModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage())
                    .addClass(SetupResource.class))
            .overrideConfigKey("quarkus.modulith.test.module", ShippingService.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.test.bootstrap-mode", "direct-dependencies")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Test
    void startsTheModuleAndItsDependencies() {
        // act
        boolean shipping = Arc.container().instance(ShippingService.class).isAvailable();

        // assert
        assertThat(shipping).isTrue();
        assertThat(SetupResource.read()).isEqualTo("module=shipping\nbootstrap-mode=DIRECT_DEPENDENCIES\n"
                + "dependencies=catalog,order\nextra-includes=\nshared-modules=shared\n");
        assertThat(Arc.container().instance(CatalogProductLookup.class).isAvailable()).isTrue();
        assertThat(Arc.container().instance(OrderManagement.class).isAvailable()).isTrue();
        assertThat(Arc.container().instance(InventoryManagement.class).isAvailable()).isFalse();
    }
}
