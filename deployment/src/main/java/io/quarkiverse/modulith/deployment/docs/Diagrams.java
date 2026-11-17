package io.quarkiverse.modulith.deployment.docs;

import java.util.Set;

import io.quarkiverse.modulith.core.util.MermaidDiagram;
import io.quarkiverse.modulith.deployment.model.ModuleDescriptor;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.deployment.model.ModuleReference;

/**
 * Renders the module model as Mermaid diagrams. The PlantUML diagrams come from the
 * {@link io.quarkiverse.modulith.docs.Documenter}.
 */
public final class Diagrams {

    private Diagrams() {
    }

    /**
     * A module listens to another when every reference to it is an event type it listens to.
     */
    public static Relation relation(ModuleDescriptor from, Set<ModuleReference> references) {
        boolean onlyEvents = references.stream().allMatch(r -> from.listenedEvents().contains(r.target()));
        return onlyEvents ? Relation.LISTENS_TO : Relation.USES;
    }

    /**
     * @return the Mermaid flowchart of the core {@link MermaidDiagram}
     */
    public static String mermaid(ModuleModel model) {
        return MermaidDiagram.of(model.applicationModules());
    }
}
