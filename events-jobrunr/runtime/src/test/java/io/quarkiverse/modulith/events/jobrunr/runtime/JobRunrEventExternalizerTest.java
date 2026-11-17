package io.quarkiverse.modulith.events.jobrunr.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;

class JobRunrEventExternalizerTest {

    private final RecordingJobs jobs = new RecordingJobs();
    private final CountingListenerExecutor executor = new CountingListenerExecutor();

    @ParameterizedTest
    @CsvSource({ "true, true, 0", "true, false, 0", "false, true, 0", "false, false, 1" })
    void enqueuesInTheTransactionOrOffTheIoThread(boolean transaction, boolean blocking, int offloaded) {
        // arrange
        JobRunrEventExternalizer externalizer = new JobRunrEventExternalizer(EventExternalizationConfiguration
                .externalizing().select(String.class::isInstance).build(), jobs, executor, () -> transaction,
                () -> blocking);

        // act
        externalizer.onEvent("o-1");
        externalizer.onEvent(42);

        // assert
        assertThat(jobs.enqueued).containsExactly("o-1");
        assertThat(executor.executed).hasValue(offloaded);
    }
}
