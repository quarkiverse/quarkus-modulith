package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ApplicationModulesFactory;
import io.quarkiverse.modulith.deployment.fixtures.nested.order.sub.SubService;
import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A module test builds its model with the {@link ApplicationModulesFactory} the class path registers, like Spring
 * Modulith's {@code ModuleTestExecution}. The application's {@code @Modulith} type is the valid fixture, which has no
 * module {@code order.sub}: only the factory's model of the nested fixture has it.
 */
public class RegisteredModulesFactoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage())
                    .addPackages(true, NestedModulesFactory.NESTED)
                    .addClass(NestedModulesFactory.class)
                    .addAsResource(new StringAsset(NestedModulesFactory.class.getName()),
                            "META-INF/services/" + ApplicationModulesFactory.class.getName()))
            .overrideConfigKey("quarkus.modulith.test.module", "order.sub")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void testsAModuleOfTheFactoryModel() {
        // act
        String basePackage = modules.getBasePackage();

        // assert
        assertThat(basePackage).isEqualTo(NestedModulesFactory.NESTED);
        assertThat(modules.getModuleByName("order.sub").map(ApplicationModule::getName)).hasValue("order.sub");
        assertThat(Arc.container().instance(SubService.class).isAvailable()).isTrue();
    }
}
