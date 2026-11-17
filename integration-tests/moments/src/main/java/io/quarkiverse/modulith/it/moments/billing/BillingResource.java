package io.quarkiverse.modulith.it.moments.billing;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Lists the moments the billing module received, one per line in alphabetical order, and forgets them.
 */
@Path("/billing/moments")
@Produces(MediaType.TEXT_PLAIN)
public class BillingResource {

    @Inject
    ReceivedMoments received;

    @GET
    public String received() {
        return String.join("\n", received.sorted());
    }

    @DELETE
    public void clear() {
        received.clear();
    }
}
