package io.quarkiverse.modulith.it.externalizationjobrunr.delivery;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;

/**
 * Exposes the deliveries and the JobRunr jobs, so that tests over HTTP can check them.
 */
@Path("/deliveries")
public class DeliveryResource {

    private static final String BROKER = "broker";
    private static final String WAREHOUSE = "warehouse";

    @Inject
    BrokerTransport broker;

    @Inject
    WarehouseTransport warehouse;

    @Inject
    Externalizations externalizations;

    @Inject
    StorageProvider jobs;

    @GET
    @Path("/{transport}")
    public List<Delivery> deliveries(@PathParam("transport") String transport) {
        return recorder(transport).deliveries();
    }

    @GET
    @Path("/{transport}/attempts")
    public int attempts(@PathParam("transport") String transport) {
        return recorder(transport).attempts();
    }

    @POST
    @Path("/{transport}/failures/{times}")
    public void failNext(@PathParam("transport") String transport, @PathParam("times") int times) {
        recorder(transport).failNext(times);
    }

    @DELETE
    public void reset() {
        broker.recorder.reset();
        warehouse.recorder.reset();
        externalizations.reset();
    }

    @GET
    @Path("/externalized")
    public List<String> externalized() {
        return externalizations.brokerResults();
    }

    @GET
    @Path("/jobs")
    public Map<String, Long> jobs() {
        long pending = Stream.of(StateName.AWAITING, StateName.SCHEDULED, StateName.ENQUEUED, StateName.PROCESSING)
                .mapToLong(jobs::countJobs).sum();
        return Map.of("pending", pending, "succeeded", jobs.countJobs(StateName.SUCCEEDED),
                "failed", jobs.countJobs(StateName.FAILED));
    }

    private Recorder recorder(String transport) {
        return switch (transport) {
            case BROKER -> broker.recorder;
            case WAREHOUSE -> warehouse.recorder;
            default -> throw new NotFoundException("Unknown transport " + transport);
        };
    }
}
