package io.quarkiverse.modulith.it.externalizationjms.broker;

public record Externalization(String eventType, String target, String brokerResult) {
}
