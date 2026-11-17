package io.quarkiverse.modulith.events.deployment.externalization.fixtures.prod;

import java.util.UUID;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class RedeliveryClient {

    private static final Logger LOG = Logger.getLogger(RedeliveryClient.class);

    @Inject
    EventRedelivery redelivery;

    void onStart(@Observes StartupEvent startup) {
        boolean resubmitted = redelivery.redeliver(UUID.fromString("7f527b53-2ffe-4fe9-bca7-b870e92f9eb1"),
                "com.acme.Unknown.on(Event)", "event");
        LOG.infof("Redelivery of an unknown listener: %s", resubmitted);
    }
}
