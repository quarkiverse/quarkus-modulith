package io.quarkiverse.modulith.events.fixtures.app.audit;

@Topic("::k-2")
public record AuditUntargeted(String id) {
}
