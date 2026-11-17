package io.quarkiverse.modulith.flyway.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FlywayModulesTest {

    @Test
    void keepsTheModuleList() {
        // arrange
        List<String> identifiers = new ArrayList<>(List.of("shipping", "billing"));

        // act
        FlywayModules modules = new FlywayModules(identifiers);
        identifiers.add("audit");

        // assert
        assertThat(modules.identifiers()).containsExactly("shipping", "billing");
    }

    @Test
    void cannotBeModified() {
        // arrange
        FlywayModules modules = new FlywayModules(List.of("shipping"));

        // act & assert
        assertThatThrownBy(() -> modules.identifiers().add("audit")).isInstanceOf(UnsupportedOperationException.class);
    }
}
