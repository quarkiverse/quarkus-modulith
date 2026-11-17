package io.quarkiverse.modulith.events.messaging.runtime;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Singleton;

import org.apache.kafka.common.header.internals.RecordHeader;

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata.OutgoingKafkaRecordMetadataBuilder;

/**
 * Kafka metadata, like Spring Modulith's Kafka externalization: the routing key becomes the record key and the headers
 * become record headers. Through the default channel the target becomes the topic, otherwise the channel's topic
 * applies, which defaults to the channel name, that is, the target. Registered only with
 * {@code quarkus-messaging-kafka}.
 */
@Singleton
public class KafkaConnectorMetadata implements ConnectorMetadata {

    @Override
    public Object create(OutgoingRouting routing) {
        OutgoingKafkaRecordMetadataBuilder<String> builder = OutgoingKafkaRecordMetadata.<String> builder()
                .withHeaders(headers(routing.headers()));
        routing.target().getKey().ifPresent(builder::withKey);
        if (routing.dynamicTarget()) {
            builder.withTopic(routing.target().getTarget());
        }
        return builder.build();
    }

    /**
     * @return record headers: byte arrays as they are, other values as their UTF-8 string form
     */
    static List<RecordHeader> headers(Map<String, Object> headers) {
        return headers.entrySet().stream()
                .map(header -> new RecordHeader(header.getKey(), value(header.getValue())))
                .toList();
    }

    private static byte[] value(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        return Objects.isNull(value) ? null : value.toString().getBytes(StandardCharsets.UTF_8);
    }
}
