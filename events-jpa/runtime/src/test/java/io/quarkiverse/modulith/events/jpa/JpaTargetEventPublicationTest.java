package io.quarkiverse.modulith.events.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.EventPublication.Status;

class JpaTargetEventPublicationTest {

    static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void readsEveryColumnAndDeserializesTheEventOnce() {
        // arrange
        NoteSerializer serializer = new NoteSerializer();
        JpaEventPublication row = row(Status.RESUBMITTED, 3, T0.plusSeconds(5), null);

        // act
        JpaTargetEventPublication publication = new JpaTargetEventPublication(row, serializer);
        Object first = publication.getEvent();
        Object second = publication.getEvent();

        // assert
        assertThat(first).isEqualTo(new Note("hello")).isSameAs(second);
        assertThat(serializer.reads).hasValue(1);
        assertThat(publication.getIdentifier()).isEqualTo(row.id);
        assertThat(publication.getEventType()).isEqualTo(Note.class.getName());
        assertThat(publication.getListenerId()).isEqualTo("l");
        assertThat(publication.getPublicationDate()).isEqualTo(T0);
        assertThat(publication.getStatus()).isEqualTo(Status.RESUBMITTED);
        assertThat(publication.getCompletionAttempts()).isEqualTo(3);
        assertThat(publication.getLastResubmissionDate()).hasValue(T0.plusSeconds(5));
        assertThat(publication.getCompletionDate()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "true, COMPLETED", "false, FAILED" })
    void derivesTheStatusOfRowsWithoutOneLikeSpringModulith(boolean completed, Status expected) {
        // arrange
        JpaEventPublication row = row(null, null, null, completed ? T0.plusSeconds(9) : null);

        // act
        JpaTargetEventPublication publication = new JpaTargetEventPublication(row, new NoteSerializer());

        // assert
        assertThat(publication.getStatus()).isEqualTo(expected);
        assertThat(publication.getCompletionAttempts()).isZero();
        assertThat(publication.isCompleted()).isEqualTo(completed);
    }

    @Test
    void failsToReadAnEventWhoseClassIsGone() {
        // arrange
        JpaEventPublication row = row(Status.PUBLISHED, 0, null, null);
        row.eventType = "com.acme.Removed";
        JpaTargetEventPublication publication = new JpaTargetEventPublication(row, new NoteSerializer());

        // act & assert
        assertThatThrownBy(publication::getEvent).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot read event publication " + row.id + " of type com.acme.Removed")
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    static JpaEventPublication row(Status status, Integer attempts, Instant resubmitted, Instant completed) {
        JpaEventPublication row = new JpaEventPublication();
        row.id = UUID.randomUUID();
        row.listenerId = "l";
        row.eventType = Note.class.getName();
        row.serializedEvent = "hello";
        row.publicationDate = T0;
        row.completionDate = completed;
        row.status = status;
        row.completionAttempts = attempts;
        row.lastResubmissionDate = resubmitted;
        return row;
    }
}
