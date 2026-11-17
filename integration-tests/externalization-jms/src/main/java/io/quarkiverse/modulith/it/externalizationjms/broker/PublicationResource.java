package io.quarkiverse.modulith.it.externalizationjms.broker;

import java.util.List;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.spi.EventRedelivery;

/**
 * Exposes the event publications kept in memory and resubmits the incomplete ones like the event publication registry.
 */
@Path("/publications")
public class PublicationResource {

    @Inject
    EventPublicationRepository repository;

    @Inject
    EventRedelivery redelivery;

    @Inject
    Externalizations externalizations;

    @GET
    public List<Publication> all() {
        return Stream.concat(repository.findCompletedPublications().stream(),
                repository.findIncompletePublications().stream()).map(PublicationResource::toPublication).toList();
    }

    @POST
    @Path("/resubmit")
    public int resubmit() {
        int resubmitted = 0;
        for (TargetEventPublication publication : repository.findIncompletePublications()) {
            if (redelivery.redeliver(publication.getIdentifier(), publication.getListenerId(),
                    publication.getEvent())) {
                resubmitted++;
            }
        }
        return resubmitted;
    }

    @GET
    @Path("/externalized")
    public List<Externalization> externalized() {
        return externalizations.received();
    }

    private static Publication toPublication(TargetEventPublication publication) {
        return new Publication(publication.getIdentifier().toString(), publication.getListenerId(),
                publication.getStatus().name(), publication.getCompletionAttempts(),
                publication.getEvent().toString());
    }
}
