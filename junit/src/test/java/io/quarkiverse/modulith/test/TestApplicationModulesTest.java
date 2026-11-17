package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.test.slice.SliceApplication;

class TestApplicationModulesTest {

    @Test
    void buildsTheModelOfTestFixturesFromAType() {
        // act
        ApplicationModules modules = TestApplicationModules.of(SliceApplication.class);

        // assert
        assertThat(modules.getBasePackage()).isEqualTo("io.quarkiverse.modulith.test.slice");
        assertThat(modules.stream().map(ApplicationModule::getName).toList())
                .containsExactlyInAnyOrderElementsOf(List.of("invoicing", "billing.nested", "shipping"));
    }

    @Test
    void buildsTheModelOfTestFixturesFromAPackage() {
        // act
        ApplicationModules modules = TestApplicationModules.of("io.quarkiverse.modulith.test.slice");

        // assert
        assertThat(modules.getModuleByName("invoicing").map(ApplicationModule::getBasePackage))
                .contains("io.quarkiverse.modulith.test.slice.billing");
        assertThat(modules.getModuleByName("shipping").map(ApplicationModule::getBasePackage))
                .contains("io.quarkiverse.modulith.test.slice.shipping");
    }
}
