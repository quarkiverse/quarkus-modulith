package io.quarkiverse.modulith.it.order.internal;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkiverse.modulith.it.order.OrderManagement;
import io.quarkiverse.modulith.it.order.events.Address;

@Path("/orders")
public class OrderResource {

    @Inject
    OrderManagement orders;

    @POST
    public Map<String, String> place(PlaceOrder request) {
        return Map.of("id", orders.place(request.sku(), request.quantity()));
    }

    @POST
    @Path("/{id}/shipment")
    public void ship(@PathParam("id") String id, Address address) {
        orders.ship(id, address, List.of(id + "-parcel-1", id + "-parcel-2"));
    }
}
