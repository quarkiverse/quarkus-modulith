package io.quarkiverse.modulith.deployment.docs;

import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.CanvasOptions;
import io.quarkiverse.modulith.docs.DiagramOptions;
import io.quarkiverse.modulith.docs.JMoleculesGroupings;
import io.quarkiverse.modulith.docs.SpringGroupings;

/**
 * Turns {@code quarkus.modulith.docs.*} into documenter options.
 */
public final class DocsOptions {

    private DocsOptions() {
    }

    public static DiagramOptions diagram(DocsConfig.Diagram config) {
        DiagramOptions options = DiagramOptions.defaults()
                .withStyle(config.style())
                .withDependencyDepth(config.dependencyDepth())
                .withElementsWithoutRelationships(config.elementsWithoutRelationships());
        return config.dependencyTypes()
                .map(types -> options.withDependencyTypes(types.toArray(DependencyType[]::new)))
                .orElse(options);
    }

    public static CanvasOptions canvas(DocsConfig.Canvas config) {
        CanvasOptions options = switch (config.groupings()) {
            case QUARKUS -> CanvasOptions.defaults();
            case SPRING -> CanvasOptions.withoutDefaultGroupings().groupingBy(JMoleculesGroupings.getGroupings())
                    .groupingBy(SpringGroupings.getGroupings());
            case NONE -> CanvasOptions.withoutDefaultGroupings();
        };
        if (config.revealInternals()) {
            options = options.revealInternals();
        }
        if (config.revealEmptyLines()) {
            options = options.revealEmptyLines();
        }
        return config.apiBase().map(options::withApiBase).orElse(options);
    }

}
