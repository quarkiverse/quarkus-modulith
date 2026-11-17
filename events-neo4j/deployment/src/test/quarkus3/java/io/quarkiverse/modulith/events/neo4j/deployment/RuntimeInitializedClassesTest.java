package io.quarkiverse.modulith.events.neo4j.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * Only native builds consume the build step, so the JVM tests never run it.
 */
class RuntimeInitializedClassesTest {

    @Test
    void defersNothingOnQuarkus3() {
        // act
        List<RuntimeInitializedClassBuildItem> classes = new EventsNeo4jProcessor().runtimeInitializedClasses();

        // assert
        assertThat(classes).isEmpty();
    }
}
