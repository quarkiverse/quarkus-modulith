package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JtaTransactionSupportTest {

    final FakeSynchronizationRegistry registry = new FakeSynchronizationRegistry();

    @ParameterizedTest
    @MethodSource("statuses")
    void isActiveWhileTheTransactionCanStillCommit(int status, boolean active) {
        // arrange
        registry.status = status;

        // act
        boolean result = support().isActive();

        // assert
        assertThat(result).isEqualTo(active);
    }

    static List<Arguments> statuses() {
        return List.of(Arguments.of(Status.STATUS_ACTIVE, true), Arguments.of(Status.STATUS_MARKED_ROLLBACK, true),
                Arguments.of(Status.STATUS_NO_TRANSACTION, false), Arguments.of(Status.STATUS_COMMITTED, false));
    }

    @Test
    void registersAnInterposedSynchronizationThatRunsAfterTheCommit() {
        // arrange
        registry.status = Status.STATUS_ACTIVE;
        List<String> runs = new ArrayList<>();

        // act
        support().afterCommit(() -> runs.add("delivered"));
        registry.interposed.get(0).afterCompletion(Status.STATUS_COMMITTED);

        // assert
        assertThat(registry.interposed).hasSize(1);
        assertThat(runs).containsExactly("delivered");
    }

    @Test
    void dropsTheActionOfATransactionMarkedForRollback() {
        // arrange
        registry.status = Status.STATUS_MARKED_ROLLBACK;

        // act
        support().afterCommit(() -> {
        });

        // assert
        assertThat(registry.interposed).isEmpty();
    }

    JtaTransactionSupport support() {
        JtaTransactionSupport support = new JtaTransactionSupport();
        support.synchronizations = registry;
        return support;
    }
}
