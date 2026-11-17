package io.quarkiverse.modulith.it.eventsmongodb.shipping;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * Ships parcels: stores the parcel and the publication of its event in one transaction.
 */
@ApplicationScoped
public class Shipping {

    @Inject
    EventPublicationRepository publications;

    @Transactional
    public void ship(String label, boolean fail) {
        Parcel parcel = new Parcel();
        parcel.label = label;
        parcel.persist();
        publications.create(TargetEventPublication.of(new ParcelShipped(label),
                PublicationTargetIdentifier.of("billing.on-parcel-shipped"), Instant.now()));
        if (fail) {
            throw new IllegalStateException("Carrier unavailable for " + label);
        }
    }
}
