package io.quarkiverse.modulith.it.inventory.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkiverse.modulith.it.inventory.InventoryManagement;

@Path("/inventory")
public class InventoryResource {

    @Inject
    InventoryManagement inventory;

    @GET
    @Path("/{sku}")
    public int stock(@PathParam("sku") String sku) {
        return inventory.stockOf(sku);
    }
}
