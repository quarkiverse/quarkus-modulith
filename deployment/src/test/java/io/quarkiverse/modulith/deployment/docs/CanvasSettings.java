package io.quarkiverse.modulith.deployment.docs;

import java.util.Optional;

/**
 * {@link DocsConfig.Canvas} values for tests.
 */
record CanvasSettings(CanvasGroupings groupings, boolean revealInternals, boolean revealEmptyLines,
        Optional<String> apiBase) implements DocsConfig.Canvas {
}
