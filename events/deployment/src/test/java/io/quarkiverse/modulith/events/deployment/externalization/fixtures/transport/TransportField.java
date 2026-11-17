package io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport;

import jakarta.enterprise.inject.Produces;

import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

public class TransportField {

    @Produces
    EventExternalizationTransport transport = new CustomTransport();
}
