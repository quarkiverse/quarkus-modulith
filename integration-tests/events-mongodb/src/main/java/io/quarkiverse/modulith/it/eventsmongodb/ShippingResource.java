package io.quarkiverse.modulith.it.eventsmongodb;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.it.eventsmongodb.shipping.Parcel;
import io.quarkiverse.modulith.it.eventsmongodb.shipping.ParcelShipped;
import io.quarkiverse.modulith.it.eventsmongodb.shipping.Shipping;

/**
 * Drives the shipping module and the MongoDB repository over HTTP, so the same tests run against the packaged and
 * the native application.
 */
@Path("/parcels")
public class ShippingResource {

    @Inject
    Shipping shipping;

    @Inject
    EventPublicationRepository publications;

    @POST
    @Path("/{label}")
    public Response ship(@PathParam("label") String label, @QueryParam("fail") boolean fail) {
        try {
            shipping.ship(label, fail);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @GET
    public List<String> parcels() {
        return Parcel.<Parcel> listAll().stream().map(parcel -> parcel.label).sorted().toList();
    }

    @GET
    @Path("/published")
    public List<String> published() {
        return publications.findIncompletePublications().stream()
                .map(publication -> ((ParcelShipped) publication.getEvent()).label())
                .sorted()
                .toList();
    }

    @DELETE
    public void clear() {
        Parcel.deleteAll();
        publications.deletePublications(publications.findIncompletePublications().stream()
                .map(publication -> publication.getIdentifier()).toList());
    }
}
