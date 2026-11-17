/**
 * Externalizes module events through Quarkus Messaging: Kafka, RabbitMQ, AMQP 1.0 and any other connector.
 * <p>
 * The build generates one outgoing channel per static routing target of the {@code Externalized} events, plus the
 * channels and the default channel listed in the configuration. Each message carries {@link RoutingMetadata} and the
 * metadata of every connector present, so the connector that sends it finds its target, key and headers.
 */
package io.quarkiverse.modulith.events.messaging;
