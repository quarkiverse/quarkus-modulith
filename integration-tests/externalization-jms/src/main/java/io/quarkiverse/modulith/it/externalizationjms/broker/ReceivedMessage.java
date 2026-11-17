package io.quarkiverse.modulith.it.externalizationjms.broker;

import java.util.Map;

public record ReceivedMessage(String messageId, String destination, String text, Map<String, Object> properties) {
}
