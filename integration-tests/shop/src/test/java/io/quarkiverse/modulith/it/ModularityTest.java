package io.quarkiverse.modulith.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ModularityTest {

    @Inject
    ApplicationModules modules;

    @Test
    void verifiesTheModularStructure() {
        // arrange
        ApplicationModules shop = modules;

        // act
        Throwable thrown = catchThrowable(shop::verify);

        // assert
        assertThat(thrown).isNull();
    }

    @Test
    void detectsTheModules() {
        // arrange
        ApplicationModules shop = modules;

        // act
        List<String> names = shop.stream().map(ApplicationModule::getName).toList();

        // assert
        assertThat(names).containsExactly("common", "order", "inventory", "notification");
        assertThat(shop.getModuleByName("inventory").orElseThrow().getDependencies()).containsExactly("order");
        assertThat(shop.getModuleByName("order").orElseThrow().getDependencies()).containsExactly("common");
        assertThat(shop.getModuleByName("order").orElseThrow().getNamedInterfaces()).containsExactly("events");
    }

    @Test
    void rendersTheModuleDiagram() {
        // arrange
        ApplicationModules shop = modules;

        // act
        String mermaid = shop.toMermaid();

        // assert
        assertThat(mermaid).contains("inventory -. \"listens to\" .-> order");
    }
}
