package io.quarkiverse.modulith.it.moduletests.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.it.moduletests.audit.AuditLog;
import io.quarkiverse.modulith.it.moduletests.catalog.Catalog;
import io.quarkiverse.modulith.it.moduletests.inventory.Inventory;
import io.quarkiverse.modulith.it.moduletests.order.Orders;
import io.quarkiverse.modulith.test.ApplicationModuleTest;
import io.quarkiverse.modulith.test.ApplicationModuleTestProfile;
import io.quarkiverse.modulith.test.BootstrapMode;
import io.quarkiverse.modulith.test.ModuleTestExecution;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.TestProfile;

@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES, extraIncludes = "audit")
@TestProfile(ShippingModuleTest.Profile.class)
class ShippingModuleTest {

    public static class Profile extends ApplicationModuleTestProfile {
    }

    @ParameterizedTest
    @ValueSource(classes = { Shipping.class, Inventory.class, Orders.class, Catalog.class, AuditLog.class })
    void startsAllDependenciesAndTheExtraModule(Class<?> type) {
        // act
        boolean available = Arc.container().instance(type).isAvailable();

        // assert
        assertThat(available).isTrue();
    }

    @Test
    void describesTheExecution(ModuleTestExecution execution) {
        // act
        String names = execution.toString();

        // assert
        assertThat(names).isEqualTo("ModuleTestExecution[module=shipping, mode=ALL_DEPENDENCIES, "
                + "started=[shipping, inventory, order, catalog, shared, audit]]");
        assertThat(execution.getExtraIncludes()).extracting(ApplicationModule::getName).containsExactly("audit");
        assertThat(execution.getDependencies()).extracting(ApplicationModule::getName)
                .containsExactly("inventory", "order", "catalog", "shared", "audit");
        assertThat(execution.getModules().getSystemName()).isEqualTo("Module tests");
        assertThat(execution.getBasePackages()).containsExactly(
                "io.quarkiverse.modulith.it.moduletests.shipping", "io.quarkiverse.modulith.it.moduletests.inventory",
                "io.quarkiverse.modulith.it.moduletests.order", "io.quarkiverse.modulith.it.moduletests.catalog",
                "io.quarkiverse.modulith.it.moduletests.shared", "io.quarkiverse.modulith.it.moduletests.audit");
        assertThat(execution.includes("io.quarkiverse.modulith.it.moduletests.ModuleTestsApplication")).isTrue();
        execution.verify();
    }
}
