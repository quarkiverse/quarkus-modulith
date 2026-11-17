package io.quarkiverse.modulith.deployment.devui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.deployment.ModuleListenersBuildItem;
import io.quarkiverse.modulith.deployment.ModuleModelBuildItem;
import io.quarkiverse.modulith.deployment.docs.Diagrams;
import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.model.ModuleNamedInterface;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkiverse.modulith.runtime.events.ListenerData;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * Adds the Modulith card to the Dev UI: the module table, a dependency graph, violations, listeners, initializers,
 * the incomplete event publications when a registry exists, and the Mermaid source.
 */
public class ModulithDevUiProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem devUi(ModuleModelBuildItem modelItem, ModuleListenersBuildItem listenersItem,
            DocumentationModelBuildItem documentation, Capabilities capabilities) {
        CardPageBuildItem card = new CardPageBuildItem();
        Optional<ModuleModel> model = modelItem.model();
        List<Map<String, Object>> modules = model.map(ModulithDevUiProcessor::modules).orElse(List.of());
        List<Map<String, Object>> violations = model.map(ModulithDevUiProcessor::violations).orElse(List.of());
        List<Map<String, Object>> listeners = listeners(listenersItem);
        List<Map<String, Object>> initializers = initializers(documentation.getModel());

        card.addBuildTimeData("modules", modules);
        card.addBuildTimeData("initializers", initializers);
        card.addBuildTimeData("relations", model.map(ModulithDevUiProcessor::relations).orElse(List.of()));
        card.addBuildTimeData("violations", violations);
        card.addBuildTimeData("listeners", listeners);
        card.addBuildTimeData("mermaid", modelItem.getMermaid());
        card.addBuildTimeData("basePackage", model.map(ModuleModel::basePackage).orElse(""));

        card.addPage(Page.webComponentPageBuilder()
                .title("Modules")
                .icon("font-awesome-solid:cubes")
                .componentLink("qwc-modulith-modules.js")
                .staticLabel(String.valueOf(modules.size())));
        card.addPage(Page.webComponentPageBuilder()
                .title("Module graph")
                .icon("font-awesome-solid:diagram-project")
                .componentLink("qwc-modulith-graph.js"));
        card.addPage(Page.webComponentPageBuilder()
                .title("Violations")
                .icon("font-awesome-solid:triangle-exclamation")
                .componentLink("qwc-modulith-violations.js")
                .staticLabel(String.valueOf(violations.size())));
        card.addPage(Page.webComponentPageBuilder()
                .title("Listeners")
                .icon("font-awesome-solid:tower-broadcast")
                .componentLink("qwc-modulith-listeners.js")
                .staticLabel(String.valueOf(listeners.size())));
        card.addPage(Page.webComponentPageBuilder()
                .title("Initializers")
                .icon("font-awesome-solid:play")
                .componentLink("qwc-modulith-initializers.js")
                .staticLabel(String.valueOf(initializers.size())));
        if (capabilities.isPresent(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY)) {
            card.addPage(Page.webComponentPageBuilder()
                    .title("Incomplete publications")
                    .icon("font-awesome-solid:envelope-open-text")
                    .componentLink("qwc-modulith-publications.js"));
        }
        card.addPage(Page.rawDataPageBuilder("Mermaid")
                .icon("font-awesome-solid:code")
                .buildTimeDataKey("mermaid"));
        return card;
    }

    static List<Map<String, Object>> modules(ModuleModel model) {
        List<Map<String, Object>> modules = new ArrayList<>();
        for (ModuleDescriptor module : model.modules()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", module.name());
            m.put("displayName", module.displayName());
            m.put("basePackage", module.basePackage());
            m.put("type", module.open() ? "open" : "closed");
            m.put("dependencies", List.copyOf(module.dependencies()));
            m.put("allowedDependencies", module.allowedDependencies().map(List::copyOf).orElse(List.of("any")));
            m.put("namedInterfaces", module.namedInterfaces().stream().map(ModuleNamedInterface::name).toList());
            m.put("exposedTypes", List.copyOf(module.exposedTypes()));
            m.put("publishedEvents", List.copyOf(module.publishedEvents()));
            m.put("listenedEvents", List.copyOf(module.listenedEvents()));
            m.put("classCount", module.classes().size());
            modules.add(m);
        }
        return modules;
    }

    static List<Map<String, Object>> relations(ModuleModel model) {
        List<Map<String, Object>> relations = new ArrayList<>();
        for (ModuleDescriptor module : model.modules()) {
            module.referencesByModule().forEach((target, references) -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("source", module.name());
                r.put("target", target);
                r.put("type", Diagrams.relation(module, references).label());
                r.put("references", references.size());
                relations.add(r);
            });
        }
        return relations;
    }

    static List<Map<String, Object>> violations(ModuleModel model) {
        List<Map<String, Object>> violations = new ArrayList<>();
        for (Violation violation : model.violations()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("kind", violation.kind().name());
            v.put("module", violation.module());
            v.put("message", violation.message());
            violations.add(v);
        }
        return violations;
    }

    static List<Map<String, Object>> listeners(ModuleListenersBuildItem listenersItem) {
        List<Map<String, Object>> listeners = new ArrayList<>();
        for (ListenerData listener : listenersItem.getListeners()) {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("id", listener.id());
            l.put("module", listener.module());
            l.put("eventType", listener.eventType());
            l.put("async", listener.async());
            listeners.add(l);
        }
        return listeners;
    }

    static List<Map<String, Object>> initializers(DocumentationModel docs) {
        List<Map<String, Object>> initializers = new ArrayList<>();
        docs.modules().forEach(module -> docs.details(module).initializers().forEach(type -> {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("module", module.getName());
            i.put("type", type);
            initializers.add(i);
        }));
        return initializers;
    }
}
