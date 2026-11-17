package io.quarkiverse.modulith.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.runtime.events.ListenerExecutorKind;
import io.quarkus.runtime.configuration.ConfigurationException;

class ListenerExecutorCheckTest {

    @ParameterizedTest
    @CsvSource({ "WORKER, false", "WORKER, true", "VIRTUAL_THREADS, true" })
    void acceptsExecutorsThatCanRun(ListenerExecutorKind executor, boolean virtualThreadsPresent) {
        // arrange
        ListenerExecutorKind kind = executor;

        // act
        Throwable failure = catchThrowable(() -> ListenerProcessor.checkExecutor(kind, virtualThreadsPresent));

        // assert
        assertThat(failure).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "VIRTUAL_THREADS, false" })
    void namesThePropertyAndTheMissingExtension(ListenerExecutorKind executor, boolean virtualThreadsPresent) {
        // arrange
        ListenerExecutorKind kind = executor;

        // act & assert
        assertThatThrownBy(() -> ListenerProcessor.checkExecutor(kind, virtualThreadsPresent))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("quarkus.modulith.listeners.executor=virtual-threads needs the quarkus-virtual-threads"
                        + " extension: add io.quarkus:quarkus-virtual-threads to the application, or use executor=worker");
    }
}
