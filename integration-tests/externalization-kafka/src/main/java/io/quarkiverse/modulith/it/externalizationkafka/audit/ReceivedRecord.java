package io.quarkiverse.modulith.it.externalizationkafka.audit;

import java.util.Map;

public record ReceivedRecord(String topic, String key, Map<String, String> headers, String value) {
}
