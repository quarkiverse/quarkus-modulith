package io.quarkiverse.modulith.it.externalizationjms.broker;

public record Publication(String id, String listenerId, String status, int completionAttempts, String event) {
}
