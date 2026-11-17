package io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport;

import jakarta.enterprise.inject.Produces;

public class OtherProducers {

    @Produces
    String name = "other";

    @Produces
    Integer number() {
        return 1;
    }
}
