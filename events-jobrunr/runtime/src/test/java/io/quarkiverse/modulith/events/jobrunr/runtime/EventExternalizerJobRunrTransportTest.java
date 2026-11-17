package io.quarkiverse.modulith.events.jobrunr.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class EventExternalizerJobRunrTransportTest {

    private static final Duration SEND_TIMEOUT = Duration.ofMillis(20);

    @Test
    void waitsForTheAcknowledgement() {
        // arrange
        List<Object> sent = new ArrayList<>();
        EventExternalizerJobRunrTransport transport = new EventExternalizerJobRunrTransport(event -> {
            sent.add(event);
            return CompletableFuture.completedFuture("ack");
        }, SEND_TIMEOUT);

        // act
        transport.externalize("o-1");

        // assert
        assertThat(sent).containsExactly("o-1");
    }

    @Test
    void failsTheJobWhenTheSendFails() {
        // arrange
        IllegalStateException down = new IllegalStateException("broker down");
        EventExternalizerJobRunrTransport transport = new EventExternalizerJobRunrTransport(
                event -> CompletableFuture.failedFuture(down), SEND_TIMEOUT);

        // act
        Throwable thrown = catchThrowable(() -> transport.externalize("o-1"));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not externalize an event of type java.lang.String").hasCause(down);
    }

    @Test
    void failsTheJobWhenTheBrokerDoesNotAcknowledgeInTime() {
        // arrange
        EventExternalizerJobRunrTransport transport = new EventExternalizerJobRunrTransport(
                event -> new CompletableFuture<>(), SEND_TIMEOUT);

        // act
        Throwable thrown = catchThrowable(() -> transport.externalize("o-1"));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("The broker did not acknowledge an event of type java.lang.String within PT0.02S"
                        + " (quarkus.modulith.events.externalization.outbox.send-timeout)")
                .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void keepsTheInterruptWhenInterruptedWhileWaiting() {
        // arrange
        EventExternalizerJobRunrTransport transport = new EventExternalizerJobRunrTransport(
                event -> new CompletableFuture<>(), SEND_TIMEOUT);
        Thread.currentThread().interrupt();

        // act
        Throwable thrown = catchThrowable(() -> transport.externalize("o-1"));

        // assert
        assertThat(Thread.interrupted()).isTrue();
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Interrupted while externalizing an event of type java.lang.String")
                .hasCauseInstanceOf(InterruptedException.class);
    }
}
