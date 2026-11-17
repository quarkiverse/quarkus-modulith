package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("logins::#{#this.user()}")
public record Login(String user, int attempts) {
}
