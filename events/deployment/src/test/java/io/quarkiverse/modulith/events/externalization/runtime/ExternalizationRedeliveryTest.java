package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;

class ExternalizationRedeliveryTest {

    private static final UUID ID = UUID.fromString("7f527b53-2ffe-4fe9-bca7-b870e92f9eb1");

    @Test
    void resubmitsOnlyThePublicationsOfTheStrategy() {
        // arrange
        List<String> resubmitted = new ArrayList<>();
        ExternalizationStrategy strategy = new RecordingStrategy("mine", resubmitted);
        ExternalizationRedelivery redelivery = new ExternalizationRedelivery(strategy);

        // act
        boolean mine = redelivery.redeliver(ID, "mine", "event");
        boolean other = redelivery.redeliver(ID, "other", "event");

        // assert
        assertThat(mine).isTrue();
        assertThat(other).isFalse();
        assertThat(resubmitted).containsExactly(ID + " event");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void unwrapsAsynchronousFailures(boolean completion) {
        // arrange
        IllegalStateException cause = new IllegalStateException("cause");
        Throwable wrapped = completion ? new CompletionException(cause) : new ExecutionException(cause);

        // act
        Throwable unwrapped = Failures.unwrap(wrapped);
        Throwable plain = Failures.unwrap(cause);
        Throwable empty = Failures.unwrap(new CompletionException((Throwable) null));

        // assert
        assertThat(unwrapped).isSameAs(cause);
        assertThat(plain).isSameAs(cause);
        assertThat(empty).isInstanceOf(CompletionException.class);
    }
}
