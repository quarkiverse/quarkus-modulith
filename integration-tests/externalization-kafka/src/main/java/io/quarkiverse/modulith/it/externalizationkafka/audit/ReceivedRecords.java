package io.quarkiverse.modulith.it.externalizationkafka.audit;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;

@ApplicationScoped
public class ReceivedRecords {

    private final List<ReceivedRecord> records = new CopyOnWriteArrayList<>();

    @Incoming("received")
    public CompletionStage<Void> receive(Message<String> message) {
        IncomingKafkaRecordMetadata<?, ?> metadata = message.getMetadata(IncomingKafkaRecordMetadata.class)
                .orElseThrow();
        Map<String, String> headers = new TreeMap<>();
        for (Header header : metadata.getHeaders()) {
            headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
        }
        records.add(new ReceivedRecord(metadata.getTopic(), String.valueOf(metadata.getKey()), headers,
                message.getPayload()));
        return message.ack();
    }

    public List<ReceivedRecord> records() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
