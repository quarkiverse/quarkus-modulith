package io.quarkiverse.modulith.moments.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class MomentsJobTest {

    @Test
    void runsTheTickOnEveryExecution() {
        // arrange
        AtomicInteger ticks = new AtomicInteger();
        MomentsJob job = new MomentsJob(ticks::incrementAndGet);

        // act
        job.accept(null);
        job.accept(null);

        // assert
        assertThat(ticks.get()).isEqualTo(2);
    }

    @Test
    void rejectsANullTick() {
        // act
        Throwable thrown = catchThrowable(() -> new MomentsJob(null));

        // assert
        assertThat(thrown).isExactlyInstanceOf(NullPointerException.class).hasMessage("Tick must not be null!");
    }
}
