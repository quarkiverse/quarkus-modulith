package io.quarkiverse.modulith.core.fixtures.shop.order;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/orders/")
public class OrderResource {

    @GET
    @Path("{id}")
    public String get(long id) {
        return "";
    }

    @POST
    public void create() {
    }

    @Path("/items")
    public Object items() {
        return null;
    }

    public void plain() {
    }
}
