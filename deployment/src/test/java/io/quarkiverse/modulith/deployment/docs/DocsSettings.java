package io.quarkiverse.modulith.deployment.docs;

import java.util.Optional;

import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.docs.DiagramStyle;
import io.quarkiverse.modulith.docs.ElementsWithoutRelationships;

/**
 * {@link DocsConfig} values for tests that call build steps directly.
 */
public record DocsSettings(boolean enabled, boolean devMode, String outputDirectory, boolean clean, Diagram diagram,
        Canvas canvas) implements DocsConfig {

    /**
     * @return the default settings, writing to the given directory
     */
    public static DocsConfig of(String outputDirectory, boolean clean) {
        return new DocsSettings(true, false, outputDirectory, clean, new DiagramSettings(DiagramStyle.C4,
                DependencyDepth.IMMEDIATE, Optional.empty(), ElementsWithoutRelationships.HIDDEN),
                new CanvasSettings(CanvasGroupings.QUARKUS, false, false, Optional.empty()));
    }
}
