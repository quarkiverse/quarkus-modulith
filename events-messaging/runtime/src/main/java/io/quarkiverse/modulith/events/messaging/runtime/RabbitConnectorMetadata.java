package io.quarkiverse.modulith.events.messaging.runtime;

import jakarta.inject.Singleton;

import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

/**
 * RabbitMQ (AMQP 0.9.1) metadata, like Spring Modulith's AMQP externalization: the routing key becomes the RabbitMQ
 * routing key and the headers become message headers. Through the default channel the target becomes the exchange,
 * otherwise the channel's exchange applies, which defaults to the channel name, that is, the target. Registered only
 * with {@code quarkus-messaging-rabbitmq}.
 */
@Singleton
public class RabbitConnectorMetadata implements ConnectorMetadata {

    @Override
    public Object create(OutgoingRouting routing) {
        OutgoingRabbitMQMetadata.Builder builder = OutgoingRabbitMQMetadata.builder().withHeaders(routing.headers());
        routing.target().getKey().ifPresent(builder::withRoutingKey);
        routing.contentType().ifPresent(builder::withContentType);
        if (routing.dynamicTarget()) {
            builder.withExchange(routing.target().getTarget());
        }
        return builder.build();
    }
}
