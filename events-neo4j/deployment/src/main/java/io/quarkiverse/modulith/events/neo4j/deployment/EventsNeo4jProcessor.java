package io.quarkiverse.modulith.events.neo4j.deployment;

import java.util.List;

import io.quarkiverse.modulith.events.neo4j.runtime.Neo4jEventPublicationRepository;
import io.quarkiverse.modulith.events.neo4j.runtime.Neo4jStoreProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * Build steps of {@code quarkus-modulith-events-neo4j}: registers the repository on the driver of the Quarkiverse
 * Neo4j extension.
 */
class EventsNeo4jProcessor {

    static final String FEATURE = "modulith-events-neo4j";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(Neo4jStoreProducer.class, Neo4jEventPublicationRepository.class)
                .build();
    }

    /**
     * Keeps the native build of the Neo4j driver working on the Quarkus line of the build.
     */
    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Neo4jLine.runtimeInitializedClasses();
    }
}
