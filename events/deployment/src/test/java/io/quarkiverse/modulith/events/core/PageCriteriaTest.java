package io.quarkiverse.modulith.events.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;
import io.quarkiverse.modulith.events.tck.TckEvent;

class PageCriteriaTest {

    static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @ParameterizedTest
    @CsvSource({ "0, 2, true", "0, 1, false", "0, 0, false", "1, 0, true", "-1, 5, false", "10, 5, false",
            "5, 5, false" })
    void matchesPublicationsBeforeTheBoundAndAfterTheCursor(int secondsAfterT0, long idBits, boolean expected) {
        // arrange
        PageCriteria criteria = new PageCriteria(T0.plusSeconds(5),
                Optional.of(new PublicationCursor(T0, new UUID(0, 1))), 10);
        TargetEventPublication publication = publication(secondsAfterT0, new UUID(0, idBits));

        // act
        boolean matches = criteria.matches(publication);

        // assert
        assertThat(matches).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    void rejectsPagesWithoutItems(int maxItems) {
        // arrange
        Instant bound = T0;

        // act & assert
        assertThatThrownBy(() -> PageCriteria.first(bound, maxItems)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxItems must be at least 1: " + maxItems);
    }

    @Test
    void ignoresCompletedPublicationsWhenMarkingStaleOnes() {
        // arrange
        InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
        TargetEventPublication done = repository.create(TargetEventPublication.of(new TckEvent("done"),
                PublicationTargetIdentifier.of("l"), T0));
        repository.markCompleted(done.getIdentifier(), T0.plusSeconds(1));

        // act
        int marked = repository.markStalePublicationsFailed(Status.COMPLETED, T0.plusSeconds(60));

        // assert
        assertThat(marked).isZero();
        assertThat(repository.findByStatus(Status.COMPLETED)).hasSize(1);
    }

    static TargetEventPublication publication(int secondsAfterT0, UUID id) {
        return TargetEventPublication.of(id, new TckEvent("e"), TckEvent.class.getName(),
                PublicationTargetIdentifier.of("l"), T0.plusSeconds(secondsAfterT0), Optional.empty(), Status.PUBLISHED,
                0, Optional.empty());
    }
}
