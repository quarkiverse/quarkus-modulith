package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EventPublicationTest {

    private static final UUID IDENTIFIER = new UUID(0, 1);

    @ParameterizedTest
    @MethodSource("completions")
    void isCompletedWhenItHasACompletionDate(Optional<Instant> completionDate, boolean expected) {
        // arrange
        EventPublication publication = publication(completionDate);

        // act
        boolean completed = publication.isCompleted();

        // assert
        assertThat(completed).isEqualTo(expected);
    }

    static List<Object[]> completions() {
        return List.of(new Object[] { Optional.of(Instant.EPOCH), true }, new Object[] { Optional.empty(), false });
    }

    @ParameterizedTest
    @MethodSource("lastPublications")
    void takesTheLastResubmissionAsLastPublicationDate(Optional<Instant> lastResubmission, Instant expected) {
        // arrange
        EventPublication publication = publication(Optional.empty(), Instant.EPOCH, lastResubmission);

        // act
        Instant last = publication.getLastPublicationDate();

        // assert
        assertThat(last).isEqualTo(expected);
    }

    static List<Object[]> lastPublications() {
        return List.of(new Object[] { Optional.empty(), Instant.EPOCH },
                new Object[] { Optional.of(Instant.EPOCH.plusSeconds(9)), Instant.EPOCH.plusSeconds(9) });
    }

    @Test
    void exposesTheEventAsApplicationEvent() {
        // arrange
        EventPublication publication = publication(Optional.empty());

        // act
        Object event = publication.getApplicationEvent();

        // assert
        assertThat(event).isEqualTo("event");
        assertThat(publication.getIdentifier()).isEqualTo(IDENTIFIER);
    }

    @Test
    void ordersByPublicationDate() {
        // arrange
        EventPublication first = publication(Optional.empty(), Instant.EPOCH, Optional.empty());
        EventPublication later = publication(Optional.empty(), Instant.EPOCH.plusSeconds(5), Optional.empty());

        // act
        int firstToLater = first.compareTo(later);
        int laterToFirst = later.compareTo(first);

        // assert
        assertThat(firstToLater).isNegative();
        assertThat(laterToFirst).isPositive();
    }

    static EventPublication publication(Optional<Instant> completionDate) {
        return publication(completionDate, Instant.EPOCH, Optional.empty());
    }

    static EventPublication publication(Optional<Instant> completionDate, Instant publicationDate,
            Optional<Instant> lastResubmission) {
        return new EventPublication() {
            @Override
            public UUID getIdentifier() {
                return IDENTIFIER;
            }

            @Override
            public Object getEvent() {
                return "event";
            }

            @Override
            public String getEventType() {
                return String.class.getName();
            }

            @Override
            public String getListenerId() {
                return "listener";
            }

            @Override
            public Instant getPublicationDate() {
                return publicationDate;
            }

            @Override
            public Optional<Instant> getLastResubmissionDate() {
                return lastResubmission;
            }

            @Override
            public Optional<Instant> getCompletionDate() {
                return completionDate;
            }

            @Override
            public Status getStatus() {
                return Status.PUBLISHED;
            }

            @Override
            public int getCompletionAttempts() {
                return 0;
            }
        };
    }
}
