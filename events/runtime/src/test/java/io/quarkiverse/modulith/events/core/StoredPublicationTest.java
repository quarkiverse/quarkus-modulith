package io.quarkiverse.modulith.events.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;

class StoredPublicationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void deserializesTheEventOnceOnFirstAccess() {
        // arrange
        CountingSerializer serializer = new CountingSerializer();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000007");
        StoredPublication publication = new StoredPublication(id, String.class.getName(), "hello", serializer,
                PublicationTargetIdentifier.of("billing.on-order"), T0, Optional.of(T0.plusSeconds(2)),
                Status.COMPLETED, 3, Optional.of(T0.plusSeconds(1)));

        // act
        int readsBefore = serializer.reads;
        Object first = publication.getEvent();
        Object second = publication.getEvent();

        // assert
        assertThat(readsBefore).isZero();
        assertThat(first).isEqualTo("hello").isSameAs(second);
        assertThat(serializer.reads).isEqualTo(1);
        assertThat(publication.getIdentifier()).isEqualTo(id);
        assertThat(publication.getEventType()).isEqualTo("java.lang.String");
        assertThat(publication.getListenerId()).isEqualTo("billing.on-order");
        assertThat(publication.getPublicationDate()).isEqualTo(T0);
        assertThat(publication.getCompletionDate()).hasValue(T0.plusSeconds(2));
        assertThat(publication.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(publication.getCompletionAttempts()).isEqualTo(3);
        assertThat(publication.getLastResubmissionDate()).hasValue(T0.plusSeconds(1));
    }
}
