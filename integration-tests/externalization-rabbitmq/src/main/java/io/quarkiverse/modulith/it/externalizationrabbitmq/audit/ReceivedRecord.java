package io.quarkiverse.modulith.it.externalizationrabbitmq.audit;

import java.util.Map;

public record ReceivedRecord(String exchange, String routingKey, Map<String, String> headers, String contentType,
        String value) {
}
