package io.quarkiverse.modulith.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.deployment.ModuleListenersBuildItem;
import io.quarkiverse.modulith.deployment.ModuleModelBuildItem;
import io.quarkiverse.modulith.deployment.docs.DocsFixture;
import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkiverse.modulith.deployment.model.Fixtures;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;
import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkus.deployment.Capabilities;
import io.quarkus.devui.spi.page.CardPageBuildItem;

/**
 * Calls the Dev UI build steps directly for the cases the dev mode test does not start: no model, violations and an
 * event publication registry.
 */
class ModulithDevUiProcessorTest {

    static final String DDD = "io.quarkiverse.modulith.deployment.fixtures.model.ddd";
    static final String CYCLE = "io.quarkiverse.modulith.deployment.fixtures.cycle";
    static final String APP = "io.quarkiverse.modulith.deployment.docs.fixtures.app.";
    static final String CYCLE_MESSAGE = "Cycle detected: a -> b -> a\n  1. Dependencies of a\n    - Field <" + CYCLE
            + ".a.A.b> has type <" + CYCLE
            + ".b.B> in (A.java:0)\n  2. Dependencies of b\n    - Field <" + CYCLE + ".b.B.a> has type <" + CYCLE
            + ".a.A> in (B.java:0)";

    static final DocumentationModelBuildItem NO_DOCUMENTATION = new DocumentationModelBuildItem(new DocumentationModel(
            new ApplicationModules(new ModelData("", "", List.of(), List.of(), "")), List.of(), List.of(),
            DocumentationSource.none()));

    @Test
    void worksWithoutModel() {
        // arrange
        ModuleModelBuildItem model = new ModuleModelBuildItem(null, "");

        // act
        CardPageBuildItem card = new ModulithDevUiProcessor().devUi(model, new ModuleListenersBuildItem(List.of()),
                NO_DOCUMENTATION, new Capabilities(Set.of()));

        // assert
        assertThat(card.getBuildTimeData().get("basePackage").getContent()).isEqualTo("");
        assertThat(card.getPages()).extracting(it -> it.build().getTitle()).containsExactly("Modules", "Module graph",
                "Violations", "Listeners", "Initializers", "Mermaid");
    }

    @Test
    void listsViolations() {
        // arrange
        ModuleModel model = Fixtures.model(CYCLE, Optional.of(CYCLE));

        // act
        CardPageBuildItem card = new ModulithDevUiProcessor().devUi(new ModuleModelBuildItem(model, ""),
                new ModuleListenersBuildItem(List.of()), NO_DOCUMENTATION, new Capabilities(Set.of()));

        // assert
        assertThat(card.getBuildTimeData().get("violations").getContent()).isEqualTo(List.of(Map.of("kind", "CYCLE",
                "module", "a", "message", CYCLE_MESSAGE)));
    }

    @Test
    void listsNoViolationOfDisabledRules() {
        // arrange
        ModuleModel model = Fixtures.model(DDD, Optional.of(DDD));

        // act
        CardPageBuildItem card = new ModulithDevUiProcessor().devUi(new ModuleModelBuildItem(model, ""),
                new ModuleListenersBuildItem(List.of()), NO_DOCUMENTATION, new Capabilities(Set.of()));

        // assert
        assertThat(card.getBuildTimeData().get("violations").getContent()).isEqualTo(List.of());
    }

    @Test
    void listsInitializers() {
        // arrange
        DocsFixture fixture = new DocsFixture();
        DocumentationModelBuildItem documentation = new DocumentationModelBuildItem(fixture.build());

        // act
        CardPageBuildItem card = new ModulithDevUiProcessor().devUi(new ModuleModelBuildItem(fixture.model, ""),
                new ModuleListenersBuildItem(List.of()), documentation, new Capabilities(Set.of()));

        // assert
        assertThat(card.getBuildTimeData().get("initializers").getContent()).isEqualTo(List.of(Map.of("module", "order",
                "type", "io.quarkiverse.modulith.deployment.docs.fixtures.app.order.OrderInit")));
    }

    @Test
    void describesEachModule() {
        // arrange
        DocsFixture fixture = new DocsFixture();

        // act
        List<Map<String, Object>> modules = ModulithDevUiProcessor.modules(fixture.model);

        // assert
        assertThat(modules).filteredOn(it -> it.get("name").equals("order")).singleElement().isEqualTo(Map.ofEntries(
                Map.entry("name", "order"), Map.entry("displayName", "Orders"),
                Map.entry("basePackage", APP + "order"), Map.entry("type", "closed"),
                Map.entry("dependencies", List.of("catalog")), Map.entry("allowedDependencies", List.of("any")),
                Map.entry("namedInterfaces", List.of()),
                Map.entry("exposedTypes", Stream.of("Holder", "Holder$Inner", "ItemOwner", "OrderCancelled",
                        "OrderCompleted", "OrderConfig", "OrderConfig$Extra", "OrderConfig$Limits", "OrderInit",
                        "OrderService", "OrderView", "Settings", "Timeouts").map(it -> APP + "order." + it).toList()),
                Map.entry("publishedEvents", List.of(APP + "order.OrderCancelled", APP + "order.OrderCompleted",
                        APP + "order.internal.InternalEvent")),
                Map.entry("listenedEvents", List.of()), Map.entry("classCount", 18)));
    }

    @Test
    void addsThePublicationsPageWhenAPublicationStoreExists() {
        // arrange
        Capabilities registry = new Capabilities(Set.of(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY));

        // act
        CardPageBuildItem card = new ModulithDevUiProcessor().devUi(new ModuleModelBuildItem(null, ""),
                new ModuleListenersBuildItem(List.of()), NO_DOCUMENTATION, registry);

        // assert
        assertThat(card.getPages()).extracting(it -> it.build().getTitle()).contains("Incomplete publications");
    }
}
