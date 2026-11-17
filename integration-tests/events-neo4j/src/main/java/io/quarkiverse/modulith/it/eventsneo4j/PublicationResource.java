package io.quarkiverse.modulith.it.eventsneo4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.it.eventsneo4j.catalog.ProductListed;

/**
 * Drives the Neo4j repository over HTTP, so the same tests run against the packaged and the native application.
 */
@Path("/publications")
public class PublicationResource {

    @Inject
    EventPublicationRepository repository;

    @POST
    @Path("/{listener}")
    public String publish(@PathParam("listener") String listener, String sku) {
        return repository.create(TargetEventPublication.of(new ProductListed(sku),
                PublicationTargetIdentifier.of(listener), Instant.now())).getIdentifier().toString();
    }

    @POST
    @Path("/{id}/completion")
    public void complete(@PathParam("id") UUID id) {
        repository.markCompleted(id, Instant.now());
    }

    @GET
    @Path("/incomplete")
    public List<String> incomplete() {
        return skus(repository.findIncompletePublications());
    }

    @GET
    @Path("/completed")
    public List<String> completed() {
        return skus(repository.findCompletedPublications());
    }

    @GET
    @Path("/by-event/{listener}/{sku}")
    public String byEvent(@PathParam("listener") String listener, @PathParam("sku") String sku) {
        return repository.findIncompletePublicationsByEventAndTargetIdentifier(new ProductListed(sku),
                PublicationTargetIdentifier.of(listener)).map(publication -> publication.getIdentifier().toString())
                .orElse("none");
    }

    @DELETE
    public void clear() {
        repository.deleteCompletedPublications();
        repository.deletePublications(repository.findIncompletePublications().stream()
                .map(TargetEventPublication::getIdentifier).toList());
    }

    private static List<String> skus(List<TargetEventPublication> publications) {
        return publications.stream()
                .map(publication -> publication.getListenerId() + ":" + ((ProductListed) publication.getEvent()).sku())
                .toList();
    }
}
