package io.quarkiverse.modulith.it.externalizationkafka.order;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("/orders/{id}")
public class OrderResource {

    @Inject
    OrderService orders;

    @POST
    @Path("/placed")
    public void placed(@PathParam("id") String id, @QueryParam("tenant") String tenant) {
        orders.place(new OrderPlaced(id, tenant));
    }

    @POST
    @Path("/shipped")
    public void shipped(@PathParam("id") String id) {
        orders.ship(new OrderShipped(id));
    }

    @POST
    @Path("/rerouted")
    public void rerouted(@PathParam("id") String id, @QueryParam("region") String region) {
        orders.reroute(new OrderRerouted(region, id));
    }
}
