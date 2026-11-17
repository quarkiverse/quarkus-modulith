package io.quarkiverse.modulith.it.externalizationamqp.audit;

import java.util.Map;

public record ReceivedRecord(String address, String subject, Map<String, String> properties, String contentType,
        String value) {
}
