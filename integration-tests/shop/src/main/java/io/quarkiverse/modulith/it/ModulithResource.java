package io.quarkiverse.modulith.it;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.IncompleteEventPublications;

/**
 * Exposes the module model and the event publication registry, so that the same tests run against the JVM and the
 * native build.
 */
@Path("/modulith")
public class ModulithResource {

    @Inject
    ApplicationModules modules;

    @Inject
    IncompleteEventPublications incomplete;

    @Inject
    CompletedEventPublications completed;

    @GET
    @Path("/modules")
    public List<Map<String, Object>> modules() {
        return modules.stream()
                .map(m -> Map.<String, Object> of("name", m.getName(), "dependencies", m.getDependencies(), "open",
                        m.isOpen()))
                .toList();
    }

    @GET
    @Path("/violations")
    public int violations() {
        return modules.detectViolations().getViolations().size();
    }

    @GET
    @Path("/publications/incomplete")
    public List<Map<String, Object>> incompletePublications() {
        return incomplete.findIncompletePublications().stream()
                .map(p -> Map.<String, Object> of("listenerId", p.getListenerId(), "status", p.getStatus().name(),
                        "attempts", p.getCompletionAttempts(), "eventType", p.getEventType()))
                .toList();
    }

    @GET
    @Path("/publications/completed")
    public int completedPublications() {
        return completed.findAll().size();
    }

    @POST
    @Path("/publications/resubmit")
    public int resubmit() {
        return incomplete.resubmitIncompletePublications(p -> true);
    }
}
