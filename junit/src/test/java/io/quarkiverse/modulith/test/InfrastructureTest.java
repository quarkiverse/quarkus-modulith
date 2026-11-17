package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InfrastructureTest {

    @Test
    void pollingExecutorForwardsTasksAndKeepsTheExecutorRunning() {
        // arrange
        List<String> ran = new ArrayList<>();
        PollingExecutor executor = new PollingExecutor(Runnable::run);

        // act
        executor.execute(() -> ran.add("task"));
        executor.shutdown();

        // assert
        assertThat(ran).containsExactly("task");
        assertThat(executor.shutdownNow()).isEmpty();
        assertThat(executor.isShutdown()).isFalse();
        assertThat(executor.isTerminated()).isFalse();
        assertThat(executor.awaitTermination(1, TimeUnit.MILLISECONDS)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "true, io.quarkiverse.modulith.test.QuarkusTransactionOperations",
            "false, io.quarkiverse.modulith.test.DirectTransactionOperations" })
    void transactionOperationsFollowNarayana(boolean narayana, Class<?> expected) {
        // act
        TransactionOperations operations = TransactionOperations.create(narayana);

        // assert
        assertThat(operations).isInstanceOf(expected);
    }

    @Test
    void directOperationsRunTheCallback() {
        // arrange
        List<String> ran = new ArrayList<>();
        TransactionOperations operations = TransactionOperations.create(false);

        // act
        operations.executeWithoutResult(() -> ran.add("run"));

        // assert
        assertThat(ran).containsExactly("run");
    }

    @Test
    void directOperationsReturnTheResultOfTheCallback() {
        // arrange
        TransactionOperations operations = TransactionOperations.create(false);

        // act
        String result = operations.execute(() -> "value");

        // assert
        assertThat(result).isEqualTo("value");
    }

    @ParameterizedTest
    @CsvSource({ "java.lang.String, true", "com.acme.DoesNotExist, false",
            "org.assertj.core.api.AssertProvider, true" })
    void detectsClasses(String className, boolean present) {
        // act
        boolean result = ClassPresence.isPresent(className);

        // assert
        assertThat(result).isEqualTo(present);
    }
}
