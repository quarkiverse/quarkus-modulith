package io.quarkiverse.modulith.events.messaging.runtime;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.messaging.RoutingMetadata;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

/**
 * RabbitMQ (AMQP 0.9.1) metadata, like Spring Modulith's AMQP externalization: the routing key becomes the RabbitMQ
 * routing key and the headers become message headers. The channel's exchange applies, which defaults to the channel
 * name, that is, the target. The RabbitMQ connector of Quarkus 3 cannot choose the exchange per message, so messages
 * of the default channel go to its exchange and carry the target in the headers
 * {@link RoutingMetadata#ROUTING_TARGET_HEADER} and {@link RoutingMetadata#SPRING_ROUTING_TARGET_HEADER}, for a headers
 * exchange to route. Registered only with {@code quarkus-messaging-rabbitmq}.
 */
@Singleton
public class RabbitConnectorMetadata implements ConnectorMetadata {

    @Override
    public Object create(OutgoingRouting routing) {
        OutgoingRabbitMQMetadata.Builder builder = OutgoingRabbitMQMetadata.builder().withHeaders(routing.headers());
        routing.target().getKey().ifPresent(builder::withRoutingKey);
        routing.contentType().ifPresent(builder::withContentType);
        if (routing.dynamicTarget()) {
            builder.withHeader(RoutingMetadata.ROUTING_TARGET_HEADER, routing.target().toString())
                    .withHeader(RoutingMetadata.SPRING_ROUTING_TARGET_HEADER, routing.target().toString());
        }
        return builder.build();
    }
}
