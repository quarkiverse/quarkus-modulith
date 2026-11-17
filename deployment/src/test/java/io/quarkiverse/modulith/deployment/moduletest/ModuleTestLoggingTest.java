package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkiverse.modulith.deployment.fixtures.valid.inventory.InventoryManagement;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleTestLoggingTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage())
                    .addClass(SetupResource.class))
            .overrideConfigKey("quarkus.modulith.test.module", "catalog")
            .overrideConfigKey("quarkus.modulith.test.extra-modules", "inventory")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.log.category.\"io.quarkiverse.modulith.deployment.moduletest\".min-level",
                    "DEBUG")
            .overrideConfigKey("quarkus.log.category.\"io.quarkiverse.modulith.deployment.moduletest\".level", "DEBUG")
            .setLogRecordPredicate(r -> r.getLoggerName().equals(ModuleTests.class.getName()))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getLevel, ModuleTestLoggingTest::message)
                    .containsExactly(
                            tuple(Level.INFO, "Bootstrapping module test for 'catalog' "
                                    + "in mode STANDALONE, starting modules [catalog, inventory, shared]"),
                            tuple(Level.DEBUG,
                                    "Module under test: catalog (io.quarkiverse.modulith.deployment.fixtures.valid.catalog)"),
                            tuple(Level.DEBUG, "Dependencies: []"),
                            tuple(Level.DEBUG, "Extra modules: [inventory]"),
                            tuple(Level.DEBUG, "Shared modules: [shared]"),
                            tuple(Level.DEBUG, "Excluded module packages: "
                                    + "[io.quarkiverse.modulith.deployment.fixtures.valid.order, "
                                    + "io.quarkiverse.modulith.deployment.fixtures.valid.shipping]"),
                            tuple(Level.DEBUG, "Kept nested module packages: []")));

    @Test
    void startsTheExtraModule() {
        // act
        boolean inventory = Arc.container().instance(InventoryManagement.class).isAvailable();

        // assert
        assertThat(inventory).isTrue();
        assertThat(SetupResource.read()).isEqualTo("module=catalog\nbootstrap-mode=STANDALONE\ndependencies=\n"
                + "extra-includes=inventory\nshared-modules=shared\n");
    }

    private static String message(LogRecord record) {
        return String.format(record.getMessage(), record.getParameters());
    }
}
