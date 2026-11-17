package io.quarkiverse.modulith.events.jobrunr.deployment;

import java.util.List;

import io.quarkus.arc.deployment.ExcludedTypeBuildItem;

/**
 * The beans that make JobRunr work on this Quarkus line. Quarkus 4 only brings Jackson 3, so JobRunr gets a Jackson 3
 * JSON mapper, and the Jackson 2 mapper producer that JobRunr's extension adds is excluded: a native build would
 * otherwise link {@code JacksonJsonMapper}, which needs Jackson 2 classes that are not there.
 */
final class JobRunrLine {

    static final String JACKSON2_MAPPER_PRODUCER = "org.jobrunr.quarkus.autoconfigure.JobRunrProducer$JobRunrJacksonJsonMapperProducer";

    private JobRunrLine() {
    }

    static String[] beanClasses() {
        return new String[] { "io.quarkiverse.modulith.events.jobrunr.runtime.Jackson3JsonMapperProducer" };
    }

    static List<ExcludedTypeBuildItem> excludedTypes() {
        return List.of(new ExcludedTypeBuildItem(JACKSON2_MAPPER_PRODUCER));
    }
}
