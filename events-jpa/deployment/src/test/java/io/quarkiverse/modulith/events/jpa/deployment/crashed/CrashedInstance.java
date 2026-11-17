package io.quarkiverse.modulith.events.jpa.deployment.crashed;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;

/**
 * Leaves the table the way an instance leaves it when it dies during a delivery: a publication marked processing ten
 * minutes ago and never completed. Runs before the registry's startup jobs.
 */
@Singleton
public class CrashedInstance {

    public static final Duration AGE = Duration.ofMinutes(10);
    public static volatile UUID left;

    @Inject
    EventPublicationRepository repository;

    void leaveAProcessingPublication(@Observes @Priority(1) StartupEvent startup) {
        TargetEventPublication publication = TargetEventPublication.of(new OrderPlaced("crashed-1", 7),
                PublicationTargetIdentifier.of("invoice.on-order-placed"), Clock.systemUTC().instant().minus(AGE));
        QuarkusTransaction.requiringNew().run(() -> {
            repository.create(publication);
            repository.markProcessing(publication.getIdentifier());
        });
        left = publication.getIdentifier();
    }
}
