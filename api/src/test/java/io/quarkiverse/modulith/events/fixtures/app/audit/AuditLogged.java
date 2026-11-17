package io.quarkiverse.modulith.events.fixtures.app.audit;

@Topic("audit::k-1")
public record AuditLogged(String id) {
}
