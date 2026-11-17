package io.quarkiverse.modulith.it.externalizationkafka.audit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.apache.kafka.clients.producer.RecordMetadata;

import io.quarkiverse.modulith.events.EventExternalized;

@ApplicationScoped
public class ExternalizedRecords {

    private final List<ExternalizedRecord> records = new CopyOnWriteArrayList<>();

    void on(@Observes EventExternalized<?, ?> event) {
        RecordMetadata metadata = (RecordMetadata) event.getBrokerResult();
        records.add(new ExternalizedRecord(event.getTarget().toString(), metadata.topic(), metadata.partition(),
                metadata.offset()));
    }

    public List<ExternalizedRecord> records() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
