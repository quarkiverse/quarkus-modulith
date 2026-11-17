package io.quarkiverse.modulith.events.messaging.deployment;

import java.util.Collection;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * The Quarkus Messaging connector extensions of the application. Connector extensions declare no capability, so the
 * build looks for their runtime artifacts in the application model.
 */
public final class MessagingConnectorsBuildItem extends SimpleBuildItem {

    static final String KAFKA = "quarkus-messaging-kafka";
    static final String RABBITMQ = "quarkus-messaging-rabbitmq";
    static final String AMQP = "quarkus-messaging-amqp";
    private static final String QUARKUS = "io.quarkus";

    private final boolean kafka;
    private final boolean rabbitmq;
    private final boolean amqp;

    MessagingConnectorsBuildItem(boolean kafka, boolean rabbitmq, boolean amqp) {
        this.kafka = kafka;
        this.rabbitmq = rabbitmq;
        this.amqp = amqp;
    }

    /**
     * @return the connectors found among the runtime dependencies
     */
    static MessagingConnectorsBuildItem of(Collection<ResolvedDependency> dependencies) {
        return new MessagingConnectorsBuildItem(contains(dependencies, KAFKA), contains(dependencies, RABBITMQ),
                contains(dependencies, AMQP));
    }

    private static boolean contains(Collection<ResolvedDependency> dependencies, String artifactId) {
        return dependencies.stream().anyMatch(
                dependency -> QUARKUS.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId()));
    }

    public boolean hasKafka() {
        return kafka;
    }

    public boolean hasRabbitmq() {
        return rabbitmq;
    }

    public boolean hasAmqp() {
        return amqp;
    }
}
