package io.quarkiverse.modulith.docs;

import static io.quarkiverse.modulith.docs.TestModels.module;
import static io.quarkiverse.modulith.docs.TestModels.modules;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModules;

class DiagramModelTest {

    @Test
    void addsEachRelationshipOnce() {
        // arrange
        ApplicationModules modules = modules("Shop", module("order", List.of()), module("catalog", List.of()));
        DiagramModel model = new DiagramModel("Shop");
        DiagramComponent order = model.addComponent(modules.getModuleByName("order").orElseThrow(), "Order");
        DiagramComponent catalog = model.addComponent(modules.getModuleByName("catalog").orElseThrow(), "Catalog");

        // act
        Optional<DiagramRelationship> first = model.uses(order, catalog, "uses", "USES_COMPONENT");
        Optional<DiagramRelationship> again = model.uses(order, catalog, "uses", "USES_COMPONENT");
        Optional<DiagramRelationship> otherDescription = model.uses(order, catalog, "depends on", "DEFAULT");
        Optional<DiagramRelationship> otherDirection = model.uses(catalog, order, "uses", "USES_COMPONENT");

        // assert
        assertThat(first).contains(new DiagramRelationship(5, order, catalog, "uses", "USES_COMPONENT"));
        assertThat(again).isEmpty();
        assertThat(otherDescription).contains(new DiagramRelationship(6, order, catalog, "depends on", "DEFAULT"));
        assertThat(otherDirection).contains(new DiagramRelationship(7, catalog, order, "uses", "USES_COMPONENT"));
        assertThat(model.relationships()).hasSize(3);
        assertThat(order).hasToString("DiagramComponent[Order]");
        assertThat(order.getTags()).containsExactly("Element", "Component");
        assertThat(order.getModule().getName()).isEqualTo("order");
    }
}
