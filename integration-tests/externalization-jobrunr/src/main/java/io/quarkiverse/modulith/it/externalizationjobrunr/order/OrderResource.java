package io.quarkiverse.modulith.it.externalizationjobrunr.order;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/orders")
public class OrderResource {

    @Inject
    OrderService orders;

    @POST
    public Response place(OrderPlaced order) {
        orders.place(order);
        return Response.accepted().build();
    }

    @POST
    @Path("/rolled-back")
    public Response placeAndRollBack(OrderPlaced order) {
        try {
            orders.placeAndRollBack(order);
            return Response.serverError().build();
        } catch (IllegalStateException expected) {
            return Response.status(Response.Status.CONFLICT).entity(expected.getMessage()).build();
        }
    }
}
