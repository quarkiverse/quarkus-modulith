package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class MessagingConnectorsBuildItemTest {

    private static ResolvedDependency dependency(String groupId, String artifactId) {
        return ResolvedDependencyBuilder.newInstance().setGroupId(groupId).setArtifactId(artifactId)
                .setVersion("1.0").build();
    }

    @ParameterizedTest
    @CsvSource({
            "io.quarkus,quarkus-messaging-kafka,true,false,false",
            "io.quarkus,quarkus-messaging-rabbitmq,false,true,false",
            "io.quarkus,quarkus-messaging-amqp,false,false,true",
            "io.quarkus,quarkus-messaging,false,false,false",
            "org.acme,quarkus-messaging-kafka,false,false,false" })
    void findsTheConnectorExtensions(String groupId, String artifactId, boolean kafka, boolean rabbitmq,
            boolean amqp) {
        // act
        MessagingConnectorsBuildItem connectors = MessagingConnectorsBuildItem
                .of(List.of(dependency("io.quarkus", "quarkus-core"), dependency(groupId, artifactId)));

        // assert
        assertThat(connectors.hasKafka()).isEqualTo(kafka);
        assertThat(connectors.hasRabbitmq()).isEqualTo(rabbitmq);
        assertThat(connectors.hasAmqp()).isEqualTo(amqp);
    }
}
