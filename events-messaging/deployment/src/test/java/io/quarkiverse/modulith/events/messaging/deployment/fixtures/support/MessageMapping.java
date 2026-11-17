package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

public class MessageMapping {

    @Produces
    @Singleton
    EventExternalizationConfiguration configuration(AckTracker tracker) {
        return EventExternalizationConfiguration.defaults(ShopApplication.BASE_PACKAGE)
                .mapping(OrderPlaced.class, event -> Message.of(event.id(),
                        Metadata.of(OutgoingKafkaRecordMetadata.<String> builder().withKey("custom").build()), () -> {
                            tracker.acknowledge();
                            return CompletableFuture.completedFuture(null);
                        }))
                .build();
    }
}
