package io.quarkiverse.modulith.it.externalizationrabbitmq.audit;

import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;

final class ContentTypes {

    private ContentTypes() {
    }

    static String of(IncomingRabbitMQMetadata metadata) {
        return metadata.getContentType();
    }
}
