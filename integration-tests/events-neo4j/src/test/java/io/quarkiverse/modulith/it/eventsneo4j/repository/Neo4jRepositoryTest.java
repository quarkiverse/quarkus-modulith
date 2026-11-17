package io.quarkiverse.modulith.it.eventsneo4j.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.neo4j.runtime.EventHash;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

class Neo4jRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");
    private static final TckEventSerializer SERIALIZER = new TckEventSerializer();

    /**
     * More than one batch of a bulk delete.
     */
    private static final int COMPLETED_NODES = 1001;

    @Test
    void deleteCompletionRemovesTheNode() {
        // arrange
        EventPublicationRepository repository = repository(Neo4j.emptyServer(), CompletionMode.DELETE);
        TargetEventPublication completed = repository.create(publication("completed", 0));
        repository.create(publication("incomplete", 1));

        // act
        repository.markCompleted(completed.getIdentifier(), T0.plusSeconds(5));

        // assert
        assertThat(events(repository.findIncompletePublications())).containsExactly("incomplete");
        assertThat(repository.findCompletedPublications()).isEmpty();
    }

    @Test
    void storesEveryProperty() {
        // arrange
        Driver driver = Neo4j.emptyServer();
        EventPublicationRepository repository = repository(driver, CompletionMode.UPDATE);
        UUID id = UUID.fromString("6f1c9a52-1b0e-4c8e-9f55-2b7d7c1f3a10");

        // act
        repository.create(TargetEventPublication.of(id, new TckEvent("e-1"), TckEvent.class.getName(), LISTENER, T0,
                Optional.of(T0.plusSeconds(9)), Status.COMPLETED, 3, Optional.of(T0.plusSeconds(7))));

        // assert
        Map<String, Object> stored = driver.executableQuery("MATCH (p:Neo4jEventPublication) RETURN p").execute()
                .records().get(0).get("p").asNode().asMap();
        assertThat(stored).containsOnlyKeys("identifier", "eventSerialized", "eventHash", "eventType", "listenerId",
                "publicationDate", "completionDate", "status", "completionAttempts", "lastResubmissionDate")
                .containsEntry("identifier", id.toString())
                .containsEntry("eventSerialized", "{\"id\":\"e-1\"}")
                .containsEntry("eventHash", EventHash.sha256("{\"id\":\"e-1\"}"))
                .containsEntry("status", "COMPLETED")
                .containsEntry("completionAttempts", 3L);
        assertThat(single(repository.findCompletedPublications())).satisfies(p -> {
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(9));
            assertThat(p.getLastResubmissionDate()).hasValue(T0.plusSeconds(7));
            assertThat(p.getCompletionAttempts()).isEqualTo(3);
        });
    }

    @Test
    void readsPublicationsOfUnknownEventTypesUntilTheirEventIsUsed() {
        // arrange
        EventPublicationRepository repository = repository(Neo4j.emptyServer(), CompletionMode.UPDATE);
        repository.create(TargetEventPublication.of(UUID.randomUUID(), new TckEvent("gone"), "com.acme.Removed",
                LISTENER, T0, Optional.empty(), Status.PUBLISHED, 0, Optional.empty()));
        repository.create(publication("known", 1));

        // act
        List<TargetEventPublication> incomplete = repository.findIncompletePublications();
        Throwable thrown = catchThrowable(() -> incomplete.get(0).getEvent());

        // assert
        assertThat(incomplete).extracting(TargetEventPublication::getEventType)
                .containsExactly("com.acme.Removed", TckEvent.class.getName());
        assertThat(incomplete.get(1).getEvent()).isEqualTo(new TckEvent("known"));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Cannot read event publication ").hasMessageEndingWith(" of type com.acme.Removed");
    }

    @Test
    void pagesWithoutAnUpperBound() {
        // arrange
        EventPublicationRepository repository = repository(Neo4j.emptyServer(), CompletionMode.UPDATE);
        repository.create(publication("first", 0));
        repository.create(publication("second", 1));

        // act
        List<TargetEventPublication> page = repository.findIncompletePublications(PageCriteria.first(Instant.MAX, 10));

        // assert
        assertThat(events(page)).containsExactly("first", "second");
    }

    @Test
    void readsNodesWrittenBySpringModulith() {
        // arrange
        Driver driver = Neo4j.emptyServer();
        EventPublicationRepository repository = repository(driver, CompletionMode.UPDATE);
        UUID open = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID done = UUID.fromString("00000000-0000-0000-0000-000000000002");
        springNode(driver, open, "open", EventHash.md5(SERIALIZER.serialize(new TckEvent("open")).toString()), null);
        springNode(driver, done, "done", EventHash.sha256(SERIALIZER.serialize(new TckEvent("done")).toString()),
                T0.plusSeconds(4));

        // act
        List<TargetEventPublication> failed = repository.findFailedPublications(FailedCriteria.all());
        Optional<TargetEventPublication> byMd5 = repository
                .findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("open"), LISTENER);
        List<TargetEventPublication> completed = repository.findCompletedPublications();

        // assert
        assertThat(single(failed)).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(open);
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
            assertThat(p.getCompletionAttempts()).isZero();
            assertThat(p.getLastResubmissionDate()).isEmpty();
        });
        assertThat(byMd5).map(TargetEventPublication::getIdentifier).hasValue(open);
        assertThat(single(completed)).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(done);
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
        });
    }

    @Test
    void deletesCompletedPublicationsInBatches() {
        // arrange
        Driver driver = Neo4j.emptyServer();
        EventPublicationRepository repository = repository(driver, CompletionMode.UPDATE);
        driver.executableQuery("UNWIND range(1, $count) AS i CREATE (p:Neo4jEventPublication {identifier: "
                + "toString(i), completionDate: $date})")
                .withParameters(Map.of("count", COMPLETED_NODES, "date", T0.atOffset(ZoneOffset.UTC))).execute();

        // act
        int deleted = repository.deleteCompletedPublicationsCompletedBefore(T0.plusSeconds(1));

        // assert
        assertThat(deleted).isEqualTo(COMPLETED_NODES);
        assertThat(driver.executableQuery("MATCH (p:Neo4jEventPublication) RETURN count(p) AS c").execute().records()
                .get(0).get("c").asInt()).isZero();
    }

    @Test
    void archivesAPublicationCompletedTwiceOnce() {
        // arrange
        Driver driver = Neo4j.emptyServer();
        EventPublicationRepository repository = repository(driver, CompletionMode.ARCHIVE);
        TargetEventPublication publication = repository.create(publication("e-1", 0));
        repository.markCompleted(publication.getIdentifier(), T0.plusSeconds(5));

        // act
        repository.markCompleted(publication.getIdentifier(), T0.plusSeconds(9));

        // assert
        assertThat(single(repository.findCompletedPublications()).getCompletionDate()).hasValue(T0.plusSeconds(5));
    }

    /**
     * Writes a node the way Spring Modulith 1.x did: no status, attempts or resubmission date.
     */
    private static void springNode(Driver driver, UUID id, String event, String hash, Instant completionDate) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", id.toString());
        properties.put("eventSerialized", SERIALIZER.serialize(new TckEvent(event)).toString());
        properties.put("eventHash", hash);
        properties.put("eventType", TckEvent.class.getName());
        properties.put("listenerId", LISTENER.value());
        properties.put("publicationDate", T0.atOffset(ZoneOffset.UTC));
        properties.put("completionDate", Optional.ofNullable(completionDate).map(date -> date.atOffset(ZoneOffset.UTC))
                .orElse(null));
        driver.executableQuery("CREATE (p:Neo4jEventPublication) SET p = $properties")
                .withParameters(Map.of("properties", properties)).execute();
    }

    private static EventPublicationRepository repository(Driver driver, CompletionMode completion) {
        return Neo4j.repository(driver, completion);
    }

    private static TargetEventPublication publication(String event, int secondsAfterT0) {
        return TargetEventPublication.of(new TckEvent(event), LISTENER, T0.plusSeconds(secondsAfterT0));
    }

    private static TargetEventPublication single(List<TargetEventPublication> publications) {
        assertThat(publications).hasSize(1);
        return publications.get(0);
    }

    private static List<String> events(List<TargetEventPublication> publications) {
        return publications.stream().map(p -> ((TckEvent) p.getEvent()).id()).toList();
    }
}
