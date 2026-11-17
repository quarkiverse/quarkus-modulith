package io.quarkiverse.modulith.runtime.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication;

class ModulithJsonRpcServiceTest {

    static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void listsPublicationsOldestFirstWithoutPayload() {
        // arrange
        ModulithJsonRpcService service = service();

        // act
        List<Map<String, Object>> publications = service.getIncompletePublications();

        // assert
        assertThat(publications).containsExactly(
                Map.of("id", FIRST.toString(), "eventType", "com.acme.OrderCompleted", "listenerId",
                        "com.acme.Inventory.on(com.acme.OrderCompleted)", "publicationDate", "2026-01-01T00:00:00Z",
                        "status", "FAILED", "completionAttempts", 2, "lastResubmissionDate", "2026-01-03T00:00:00Z"),
                Map.of("id", SECOND.toString(), "eventType", "com.acme.OrderCompleted", "listenerId",
                        "com.acme.Inventory.on(com.acme.OrderCompleted)", "publicationDate", "2026-01-02T00:00:00Z",
                        "status", "FAILED", "completionAttempts", 2, "lastResubmissionDate", ""));
    }

    @Test
    void listsTheOldestPublicationsOnly() {
        // arrange
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        ModulithJsonRpcService service = new ModulithJsonRpcService();
        service.publications = new RecordingPublications(IntStream.rangeClosed(0, ModulithJsonRpcService.MAX_LISTED)
                .<EventPublication> mapToObj(
                        minute -> new StoredPublication(new UUID(0, minute), start.plusSeconds(60L * minute),
                                Optional.empty()))
                .toList().reversed());

        // act
        List<Map<String, Object>> publications = service.getIncompletePublications();

        // assert
        assertThat(publications).hasSize(ModulithJsonRpcService.MAX_LISTED);
        assertThat(publications.getFirst()).containsEntry("publicationDate", "2026-01-01T00:00:00Z");
        assertThat(publications.getLast()).containsEntry("publicationDate", "2026-01-01T01:39:00Z");
    }

    @Test
    void resubmitsOneOrAllPublications() {
        // arrange
        ModulithJsonRpcService service = service();

        // act
        int one = service.resubmit(SECOND.toString());
        int unknown = service.resubmit("missing");
        int all = service.resubmitAll();

        // assert
        assertThat(one).isEqualTo(1);
        assertThat(unknown).isZero();
        assertThat(all).isEqualTo(2);
    }

    static ModulithJsonRpcService service() {
        ModulithJsonRpcService service = new ModulithJsonRpcService();
        service.publications = new RecordingPublications(List.of(
                new StoredPublication(SECOND, Instant.parse("2026-01-02T00:00:00Z"), Optional.empty()),
                new StoredPublication(FIRST, Instant.parse("2026-01-01T00:00:00Z"),
                        Optional.of(Instant.parse("2026-01-03T00:00:00Z")))));
        return service;
    }
}
