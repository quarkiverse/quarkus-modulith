package io.quarkiverse.modulith.it.eventsjdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.agroal.api.AgroalDataSource;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.it.eventsjdbc.greeting.GreetingPublished;

/**
 * Drives the JDBC repository over HTTP, so the same tests run against the packaged and the native application.
 */
@Path("/publications")
public class PublicationResource {

    @Inject
    EventPublicationRepository repository;

    @Inject
    AgroalDataSource dataSource;

    @POST
    @Path("/{listener}")
    public String publish(@PathParam("listener") String listener, String message) {
        return repository.create(TargetEventPublication.of(new GreetingPublished(message),
                PublicationTargetIdentifier.of(listener), Instant.now())).getIdentifier().toString();
    }

    @POST
    @Path("/{id}/completion")
    public void complete(@PathParam("id") UUID id) {
        repository.markCompleted(id, Instant.now());
    }

    @GET
    @Path("/incomplete")
    public List<String> incomplete() {
        return messages(repository.findIncompletePublications());
    }

    @GET
    @Path("/completed")
    public List<String> completed() {
        return messages(repository.findCompletedPublications());
    }

    @DELETE
    public void clear() {
        repository.deleteCompletedPublications();
        repository.deletePublications(repository.findIncompletePublications().stream()
                .map(TargetEventPublication::getIdentifier).toList());
    }

    @GET
    @Path("/indexes")
    public List<String> indexes() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "event_publication", false,
                        false)) {
            TreeSet<String> names = new TreeSet<>();
            while (indexes.next()) {
                names.add(indexes.getString("INDEX_NAME").toLowerCase(Locale.ROOT));
            }
            return List.copyOf(names);
        }
    }

    private static List<String> messages(List<TargetEventPublication> publications) {
        return publications.stream()
                .map(publication -> publication.getListenerId() + ":"
                        + ((GreetingPublished) publication.getEvent()).message())
                .toList();
    }
}
