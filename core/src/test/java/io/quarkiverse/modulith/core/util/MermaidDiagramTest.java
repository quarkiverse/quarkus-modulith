package io.quarkiverse.modulith.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.fixtures.diagram.DiagramApplication;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;

class MermaidDiagramTest {

    @Test
    void rendersTheModelOfAPlainTest() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

        // act
        String mermaid = modules.toMermaid();

        // assert
        assertThat(mermaid).isEqualTo("""
                flowchart LR
                    catalog["Catalog"]
                    catalog_pricing["Pricing"]
                    inventory["Stock keeping"]
                    kernel["Shared kernel"]
                    legacy["Legacy"]
                    order["Order"]
                    catalog_pricing -- "uses" --> catalog
                    inventory -- "uses" --> order
                    order -- "uses" --> catalog
                    order -- "uses" --> kernel
                    classDef open stroke-dasharray: 5 5
                    class kernel open
                """);
    }

    @Test
    void drawsListenersAsDottedArrowsAndEscapesQuotes() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(DiagramApplication.class, ClassLocations.TESTS);

        // act
        String mermaid = MermaidDiagram.of(modules);

        // assert
        assertThat(mermaid).isEqualTo("""
                flowchart LR
                    alerts["Alerts"]
                    billing["The #quot;Billing#quot; module"]
                    alerts -. "listens to" .-> billing
                """);
    }

    @ParameterizedTest
    @CsvSource({ "order, order", "9lives, m_9lives", "a-b, a_b" })
    void derivesNodeIdentifiers(String module, String id) {
        // act
        String actual = MermaidDiagram.id(module);

        // assert
        assertThat(actual).isEqualTo(id);
    }

    @Test
    void keepsModulesWithTheSameNodeIdentifierApart() {
        // arrange
        List<ModuleData> data = Stream.of("a.b", "a-b", "a_b", "a_b_2")
                .map(it -> new ModuleData(it, it, "com.acme." + it.replace(".", "dot").replace("-", "dash"), false, false,
                        List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of()))
                .toList();
        ApplicationModules modules = new ApplicationModules(new ModelData("", "com.acme", data, List.of(), ""));

        // act
        String mermaid = MermaidDiagram.of(modules);

        // assert
        assertThat(mermaid).isEqualTo("""
                flowchart LR
                    a_b["a-b"]
                    a_b_2["a.b"]
                    a_b_3["a_b"]
                    a_b_2_2["a_b_2"]
                """);
    }
}
