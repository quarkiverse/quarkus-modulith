package io.quarkiverse.modulith.it.springcompat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SpringAnnotationsTest {

    @Inject
    ApplicationModules modules;

    @Test
    void readsTheSpringModulithDeclarations() {
        // act
        List<String> names = modules.stream().map(ApplicationModule::getName).sorted().toList();

        // assert
        assertThat(names).containsExactly("auth", "billing", "common", "company", "fiscal", "mail", "notification",
                "queue", "reporting");
        assertThat(modules.getSystemName()).isEqualTo("SpringCompatApplication");
        assertThat(modules.getModuleByName("common").map(ApplicationModule::isOpen)).hasValue(true);
        assertThat(modules.getModuleByName("mail").flatMap(ApplicationModule::getAllowedDependencies))
                .hasValue(List.of("auth", "common"));
        assertThat(modules.getModuleByName("mail").map(ApplicationModule::getDependencies))
                .hasValue(List.of("auth", "common"));
        assertThat(modules.getModuleByName("reporting").map(ApplicationModule::getDisplayName)).isEqualTo(
                Optional.of("reporting"));
        assertThat(modules.detectViolations().hasViolations()).isFalse();
    }

    @Test
    void buildsTheModelOfThePlainTest() {
        // act
        ApplicationModules plain = ApplicationModules.of(SpringCompatApplication.class);

        // assert
        assertThat(plain.stream().map(it -> it.toString(plain)).toList())
                .isEqualTo(modules.stream().map(it -> it.toString(modules)).toList());
    }
}
