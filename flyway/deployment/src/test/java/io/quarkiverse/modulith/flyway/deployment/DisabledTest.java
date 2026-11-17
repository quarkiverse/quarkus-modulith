package io.quarkiverse.modulith.flyway.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.flyway.runtime.FlywayModules;
import io.quarkiverse.modulith.flyway.runtime.ModulithFlywayCustomizer;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class DisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(ShopApplication.archive())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", ShopApplication.BASE_PACKAGE)
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:disabled;DB_CLOSE_DELAY=-1");

    @Test
    void registersNoModuleMigrations() {
        // act
        boolean modules = Arc.container().instance(FlywayModules.class).isAvailable();
        boolean customizer = Arc.container().instance(ModulithFlywayCustomizer.class).isAvailable();

        // assert
        assertThat(modules).isFalse();
        assertThat(customizer).isFalse();
    }
}
