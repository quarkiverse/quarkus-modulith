package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.HashMap;

import jakarta.inject.Singleton;

import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata.OutgoingAmqpMetadataBuilder;
import io.vertx.core.json.JsonObject;

/**
 * AMQP 1.0 metadata: the routing key becomes the message subject and the headers become application properties.
 * Through the default channel the target becomes the address, otherwise the channel's address applies, which defaults
 * to the channel name, that is, the target. Registered only with {@code quarkus-messaging-amqp}.
 */
@Singleton
public class AmqpConnectorMetadata implements ConnectorMetadata {

    @Override
    public Object create(OutgoingRouting routing) {
        OutgoingAmqpMetadataBuilder builder = OutgoingAmqpMetadata.builder()
                .withApplicationProperties(new JsonObject(new HashMap<>(routing.headers())));
        routing.target().getKey().ifPresent(builder::withSubject);
        routing.contentType().ifPresent(builder::withContentType);
        if (routing.dynamicTarget()) {
            builder.withAddress(routing.target().getTarget());
        }
        return builder.build();
    }
}
