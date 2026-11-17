package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class VirtualThreadListenerExecutorTest {

    private final List<String> calls = new ArrayList<>();
    private final RecordingExecutorService worker = new RecordingExecutorService("worker", calls);
    private final RecordingExecutorService virtualThreads = new RecordingExecutorService("virtual", calls);
    private final VirtualThreadListenerExecutor executor = new VirtualThreadListenerExecutor();

    @Test
    void startsTheVirtualThreadFromAWorkerTask() {
        // arrange
        executor.worker = worker;
        executor.virtualThreads = virtualThreads;

        // act
        executor.execute(() -> calls.add("task"));

        // assert
        assertThat(calls).containsExactly("worker", "virtual", "task");
    }

    @Test
    void runsTheTaskOnTheWorkerThreadWhenTheVirtualThreadExecutorIsShutDown() {
        // arrange
        executor.worker = worker;
        executor.virtualThreads = virtualThreads;
        virtualThreads.shutdown();

        // act
        executor.execute(() -> calls.add("task"));

        // assert
        assertThat(calls).containsExactly("worker", "virtual rejected", "task");
    }
}
