package io.quarkiverse.modulith.it.externalizationkafka.audit;

public record ExternalizedRecord(String target, String topic, int partition, long offset) {
}
