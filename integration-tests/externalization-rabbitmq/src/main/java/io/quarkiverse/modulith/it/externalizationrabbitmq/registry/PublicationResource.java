package io.quarkiverse.modulith.it.externalizationrabbitmq.registry;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.spi.EventRedelivery;

@Path("/publications")
public class PublicationResource {

    @Inject
    EventPublicationRepository repository;

    @Inject
    EventRedelivery redelivery;

    @GET
    @Path("/incomplete")
    public List<PublicationView> incomplete() {
        return repository.findIncompletePublications().stream().map(PublicationView::of).toList();
    }

    @GET
    @Path("/completed")
    public List<PublicationView> completed() {
        return repository.findCompletedPublications().stream().map(PublicationView::of).toList();
    }

    @POST
    @Path("/{id}/redeliver")
    public boolean redeliver(@PathParam("id") UUID id) {
        TargetEventPublication publication = repository.findIncompletePublications().stream()
                .filter(candidate -> candidate.getIdentifier().equals(id)).findFirst()
                .orElseThrow(NotFoundException::new);
        return redelivery.redeliver(id, publication.getListenerId(), publication.getEvent());
    }

    @DELETE
    public void clear() {
        repository.deletePublications(repository.findIncompletePublications().stream()
                .map(TargetEventPublication::getIdentifier).toList());
        repository.deleteCompletedPublications();
    }
}
