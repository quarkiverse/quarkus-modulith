package io.quarkiverse.modulith.it.externalizationamqp.audit;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/audit")
public class AuditResource {

    @Inject
    ReceivedRecords received;

    @Inject
    ExternalizedRecords externalized;

    @ConfigProperty(name = "amqp-port")
    String broker;

    @GET
    @Path("/broker")
    public String broker() {
        return "localhost:" + broker;
    }

    @GET
    @Path("/received")
    public List<ReceivedRecord> received() {
        return received.records();
    }

    @GET
    @Path("/externalized")
    public List<ExternalizedRecord> externalized() {
        return externalized.records();
    }

    @DELETE
    public void clear() {
        received.clear();
        externalized.clear();
    }
}
