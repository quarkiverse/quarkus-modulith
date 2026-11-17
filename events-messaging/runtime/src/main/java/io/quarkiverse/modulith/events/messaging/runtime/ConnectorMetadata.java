package io.quarkiverse.modulith.events.messaging.runtime;

/**
 * Creates the metadata of one Quarkus Messaging connector for an outgoing message. The build registers one bean per
 * connector present, and every message carries the metadata of all of them: each connector reads its own type only.
 */
public interface ConnectorMetadata {

    /**
     * @return the connector metadata, such as Kafka's {@code OutgoingKafkaRecordMetadata}
     */
    Object create(OutgoingRouting routing);
}
