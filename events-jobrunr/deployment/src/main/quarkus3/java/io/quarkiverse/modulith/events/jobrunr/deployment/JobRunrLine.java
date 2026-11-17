package io.quarkiverse.modulith.events.jobrunr.deployment;

import java.util.List;

import io.quarkus.arc.deployment.ExcludedTypeBuildItem;

/**
 * The beans that make JobRunr work on this Quarkus line. Quarkus 3 needs none.
 */
final class JobRunrLine {

    private JobRunrLine() {
    }

    static String[] beanClasses() {
        return new String[0];
    }

    static List<ExcludedTypeBuildItem> excludedTypes() {
        return List.of();
    }
}
