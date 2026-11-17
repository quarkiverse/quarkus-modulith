package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InFlightDeliveriesTest {

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void waitsUntilTheLastDeliveryEnds() {
        // arrange
        InFlightDeliveries inFlight = new InFlightDeliveries();
        inFlight.begin();
        inFlight.begin();
        Thread.ofVirtual().start(() -> {
            inFlight.end();
            inFlight.end();
        });

        // act
        boolean idle = inFlight.awaitIdle(Duration.ofSeconds(5));

        // assert
        assertThat(idle).isTrue();
        assertThat(inFlight.count()).isZero();
    }

    @Test
    void givesUpAfterTheTimeout() {
        // arrange
        InFlightDeliveries inFlight = new InFlightDeliveries();
        inFlight.begin();

        // act
        boolean idle = inFlight.awaitIdle(Duration.ofMillis(5));

        // assert
        assertThat(idle).isFalse();
        assertThat(inFlight.count()).isEqualTo(1);
    }

    @Test
    void stopsWaitingWhenInterrupted() {
        // arrange
        InFlightDeliveries inFlight = new InFlightDeliveries();
        inFlight.begin();
        Thread.currentThread().interrupt();

        // act
        boolean idle = inFlight.awaitIdle(Duration.ofSeconds(5));

        // assert
        assertThat(idle).isFalse();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void refusesNewDeliveriesOnceStoppedButKeepsCountingForcedOnes() {
        // arrange
        InFlightDeliveries inFlight = new InFlightDeliveries();
        boolean before = inFlight.tryBegin();
        inFlight.stop();

        // act
        boolean after = inFlight.tryBegin();
        inFlight.begin();

        // assert
        assertThat(before).isTrue();
        assertThat(after).isFalse();
        assertThat(inFlight.count()).isEqualTo(2);
    }
}
