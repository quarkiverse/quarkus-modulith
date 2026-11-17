package io.quarkiverse.modulith.it.moments.clock;

import java.time.Duration;
import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.modulith.moments.support.Now;
import io.quarkiverse.modulith.moments.support.TimeMachine;

/**
 * Sets the clock and shifts time over HTTP, so that the tests also run against the packaged application.
 */
@Path("/time")
@Produces(MediaType.TEXT_PLAIN)
public class TimeResource {

    @Inject
    TestClock clock;

    @Inject
    TimeMachine timeMachine;

    @Inject
    Now now;

    @PUT
    @Path("/clock")
    public String set(String instant) {
        clock.set(Instant.parse(instant));
        timeMachine.reset();
        return now.now().toString();
    }

    @POST
    @Path("/shift")
    public String shift(String duration) {
        timeMachine.shiftBy(Duration.parse(duration));
        return now.now().toString();
    }

    @GET
    @Path("/now")
    public String now() {
        return now.now().toString();
    }
}
