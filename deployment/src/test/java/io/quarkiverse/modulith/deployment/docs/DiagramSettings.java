package io.quarkiverse.modulith.deployment.docs;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.DiagramStyle;
import io.quarkiverse.modulith.docs.ElementsWithoutRelationships;

/**
 * {@link DocsConfig.Diagram} values for tests.
 */
record DiagramSettings(DiagramStyle style, DependencyDepth dependencyDepth,
        Optional<List<DependencyType>> dependencyTypes,
        ElementsWithoutRelationships elementsWithoutRelationships) implements DocsConfig.Diagram {
}
