package io.quarkiverse.modulith.it.moments.clock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TestClockTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant LATER = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void startsInUtcAtTheFixedInstant() {
        // act
        TestClock clock = new TestClock();

        // assert
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void withZoneKeepsReadingTheInstantOfTheOriginalClock() {
        // arrange
        TestClock clock = new TestClock();
        Clock berlin = clock.withZone(BERLIN);

        // act
        clock.set(LATER);

        // assert
        assertThat(berlin.getZone()).isEqualTo(BERLIN);
        assertThat(berlin.instant()).isEqualTo(LATER);
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
