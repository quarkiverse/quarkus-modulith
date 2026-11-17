package io.quarkiverse.modulith.events.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventPublication.Status;

class SpiValuesTest {

    private static final Instant PUBLISHED = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant COMPLETED = PUBLISHED.plusSeconds(30);
    private static final Instant RESUBMITTED = PUBLISHED.plusSeconds(10);
    private static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");

    @Test
    void targetIdentifiersKeepTheirValue() {
        // arrange
        String value = "billing.on-order";

        // act
        PublicationTargetIdentifier identifier = PublicationTargetIdentifier.of(value);

        // assert
        assertThat(identifier.value()).isEqualTo(value);
        assertThat(identifier).hasToString(value);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " ", "\t" })
    void targetIdentifiersAreNeverBlank(String value) {
        // act & assert
        assertThatThrownBy(() -> PublicationTargetIdentifier.of(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newPublicationsArePublishedAndIncomplete() {
        // arrange
        String event = "event";

        // act
        TargetEventPublication publication = TargetEventPublication.of(event, LISTENER, PUBLISHED);

        // assert
        assertThat(publication.getIdentifier()).isNotEqualTo(TargetEventPublication.of(event, LISTENER, PUBLISHED)
                .getIdentifier());
        assertThat(publication.getEvent()).isEqualTo(event);
        assertThat(publication.getEventType()).isEqualTo(String.class.getName());
        assertThat(publication.getTargetIdentifier()).isEqualTo(LISTENER);
        assertThat(publication.getListenerId()).isEqualTo(LISTENER.value());
        assertThat(publication.getPublicationDate()).isEqualTo(PUBLISHED);
        assertThat(publication.getLastPublicationDate()).isEqualTo(PUBLISHED);
        assertThat(publication.getCompletionDate()).isEmpty();
        assertThat(publication.getStatus()).isEqualTo(Status.PUBLISHED);
        assertThat(publication.getCompletionAttempts()).isZero();
        assertThat(publication.isCompleted()).isFalse();
        assertThat(publication.getLastResubmissionDate()).isEmpty();
    }

    @Test
    void readBackPublicationsKeepEveryField() {
        // arrange
        UUID id = new UUID(0, 7);

        // act
        TargetEventPublication publication = TargetEventPublication.of(id, "event", "com.acme.Event", LISTENER,
                PUBLISHED, Optional.of(COMPLETED), Status.COMPLETED, 3, Optional.of(RESUBMITTED));

        // assert
        assertThat(publication.getIdentifier()).isEqualTo(id);
        assertThat(publication.getEvent()).isEqualTo("event");
        assertThat(publication.getEventType()).isEqualTo("com.acme.Event");
        assertThat(publication.getTargetIdentifier()).isEqualTo(LISTENER);
        assertThat(publication.getPublicationDate()).isEqualTo(PUBLISHED);
        assertThat(publication.getCompletionDate()).hasValue(COMPLETED);
        assertThat(publication.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(publication.getCompletionAttempts()).isEqualTo(3);
        assertThat(publication.getLastResubmissionDate()).hasValue(RESUBMITTED);
        assertThat(publication.getLastPublicationDate()).isEqualTo(RESUBMITTED);
        assertThat(publication.isCompleted()).isTrue();
    }

    @Test
    void allFailedCriteriaAreBoundedByTheFarFuture() {
        // act
        FailedCriteria all = FailedCriteria.all();

        // assert
        assertThat(all.maxItems()).isEqualTo(Integer.MAX_VALUE);
        assertThat(all.publishedBefore()).isEqualTo(EventPublicationRepository.FAR_FUTURE)
                .isBefore(Instant.parse("9999-12-31T00:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, Integer.MIN_VALUE })
    void failedCriteriaNeedOneItemAtLeast(int maxItems) {
        // act & assert
        assertThatThrownBy(() -> new FailedCriteria(maxItems, PUBLISHED)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxItems must be at least 1: " + maxItems);
    }

    @Test
    void failedCriteriaNeedABound() {
        // act & assert
        assertThatThrownBy(() -> new FailedCriteria(1, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publishedBefore must not be null");
    }
}
