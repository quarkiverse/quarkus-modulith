package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

public class ValidModulesTest {

    static final String VALID = "io.quarkiverse.modulith.deployment.fixtures.valid.";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()));

    @Inject
    ApplicationModules modules;

    @Test
    void detectsModulesFromTopLevelPackages() {
        // arrange
        ApplicationModules detected = modules;

        // act
        List<String> names = detected.stream().map(ApplicationModule::getName).toList();

        // assert
        assertThat(detected.getSystemName()).isEqualTo("Shop");
        assertThat(detected.getBasePackage()).isEqualTo(ShopApplication.class.getPackageName());
        assertThat(names).containsExactly("order", "shared", "catalog", "inventory", "shipping");
    }

    @Test
    void readsModuleDeclarations() {
        // arrange
        String name = "inventory";

        // act
        ApplicationModule inventory = modules.getModuleByName(name).orElseThrow();

        // assert
        assertThat(inventory.getDisplayName()).isEqualTo("Inventory");
        assertThat(inventory.getAllowedDependencies()).hasValue(List.of("order"));
        assertThat(modules.getModuleByName("order").orElseThrow().getAllowedDependencies()).isEmpty();
        assertThat(modules.getModuleByName("shared").orElseThrow().isOpen()).isTrue();
        assertThat(modules.getModuleByName("catalog").orElseThrow().getNamedInterfaces()).containsExactly("spi");
    }

    @Test
    void computesDependenciesFromBytecode() {
        // arrange
        List<String> names = List.of("inventory", "shipping", "catalog", "order");

        // act
        List<List<String>> dependencies = names.stream()
                .map(it -> List.copyOf(modules.getModuleByName(it).orElseThrow().getDependencies())).toList();

        // assert
        assertThat(dependencies).containsExactly(List.of("order"), List.of("catalog", "order"), List.of("shared"),
                List.of());
    }

    @Test
    void detectsPublishedAndListenedEvents() {
        // arrange
        String event = VALID + "order.OrderCompleted";

        // act
        ApplicationModule order = modules.getModuleByName("order").orElseThrow();
        ApplicationModule inventory = modules.getModuleByName("inventory").orElseThrow();

        // assert
        assertThat(order.getPublishedEvents()).containsExactly(event);
        assertThat(inventory.getListenedEvents()).containsExactly(event);
    }

    @Test
    void findsModuleOfType() {
        // arrange
        String repository = VALID + "order.internal.OrderRepository";

        // act
        Optional<ApplicationModule> module = modules.getModuleForType(repository);

        // assert
        assertThat(module).map(ApplicationModule::getName).hasValue("order");
        assertThat(modules.getModuleForType(ShopApplication.class)).isEmpty();
    }

    @Test
    void verifiesWithoutViolations() {
        // arrange
        ApplicationModules valid = modules;

        // act
        Throwable thrown = catchThrowable(valid::verify);

        // assert
        assertThat(thrown).isNull();
        assertThat(valid.detectViolations().hasViolations()).isFalse();
    }

    @Test
    void rendersMermaid() {
        // arrange
        ApplicationModules valid = modules;

        // act
        String mermaid = valid.toMermaid();

        // assert
        assertThat(mermaid)
                .startsWith("flowchart LR")
                .contains("inventory -. \"listens to\" .-> order")
                .contains("shipping -- \"uses\" --> catalog")
                .contains("class shared open");
    }
}
