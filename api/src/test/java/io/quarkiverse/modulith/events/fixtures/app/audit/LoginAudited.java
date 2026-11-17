package io.quarkiverse.modulith.events.fixtures.app.audit;

public record LoginAudited(String user) implements Audited {
}
