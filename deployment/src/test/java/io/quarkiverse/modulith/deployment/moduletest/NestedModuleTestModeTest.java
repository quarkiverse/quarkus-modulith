package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.nested.billing.BillingService;
import io.quarkiverse.modulith.deployment.fixtures.nested.order.OrderService;
import io.quarkiverse.modulith.deployment.fixtures.nested.order.internal.OrderStore;
import io.quarkiverse.modulith.deployment.fixtures.nested.order.other.OtherService;
import io.quarkiverse.modulith.deployment.fixtures.nested.order.sub.SubService;
import io.quarkiverse.modulith.deployment.fixtures.nested.order.sub.internal.SubStore;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests the module {@code order.sub}, nested inside {@code order}, which is left out with its other nested module.
 */
public class NestedModuleTestModeTest {

    static final String NESTED = "io.quarkiverse.modulith.deployment.fixtures.nested";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, NESTED))
            .overrideConfigKey("quarkus.modulith.base-package", NESTED)
            .overrideConfigKey("quarkus.modulith.test.module", "order.sub")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Test
    void startsTheNestedModuleUnderTest() {
        // act
        boolean service = Arc.container().instance(SubService.class).isAvailable();
        boolean store = Arc.container().instance(SubStore.class).isAvailable();

        // assert
        assertThat(service).isTrue();
        assertThat(store).isTrue();
    }

    @Test
    void leavesTheEnclosingAndTheOtherModulesOut() {
        // act
        boolean enclosing = Arc.container().instance(OrderService.class).isAvailable()
                || Arc.container().instance(OrderStore.class).isAvailable();
        boolean others = Arc.container().instance(OtherService.class).isAvailable()
                || Arc.container().instance(BillingService.class).isAvailable();

        // assert
        assertThat(enclosing).isFalse();
        assertThat(others).isFalse();
    }
}
