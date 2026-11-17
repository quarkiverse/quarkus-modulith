package io.quarkiverse.modulith.it.observability.order;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/orders")
public class OrderResource {

    @Inject
    OrderService orders;

    @POST
    @Path("/{id}")
    public String place(@PathParam("id") String id) {
        return orders.place(id);
    }
}
