package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.transaction.Status;
import jakarta.transaction.Transactional.TxType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TransactionSupportTest {

    private final NoTransactionSupport support = new NoTransactionSupport();
    private final List<String> runs = new ArrayList<>();

    @Test
    void isNeverActiveWithoutTransactions() {
        // act
        boolean active = support.isActive();

        // assert
        assertThat(active).isFalse();
    }

    @ParameterizedTest
    @MethodSource("runningWork")
    void runsWorkRightAwayWithoutTransactions(BiConsumer<NoTransactionSupport, Runnable> work) {
        // act
        work.accept(support, () -> runs.add("ran"));

        // assert
        assertThat(runs).containsExactly("ran");
    }

    static List<BiConsumer<NoTransactionSupport, Runnable>> runningWork() {
        return List.of(NoTransactionSupport::afterCommit, NoTransactionSupport::inNewTransaction,
                (support, work) -> support.run(TxType.REQUIRES_NEW, work));
    }

    @Test
    void returnsTheResultOfABooleanWorkWithoutTransactions() {
        // act
        boolean claimed = support.inNewTransaction(() -> runs.add("ran"));

        // assert
        assertThat(claimed).isTrue();
        assertThat(runs).containsExactly("ran");
    }

    @ParameterizedTest
    @MethodSource("callingWork")
    void returnsTheResultOfACallWithoutTransactions(BiFunction<NoTransactionSupport, Supplier<String>, String> call) {
        // act
        String result = call.apply(support, () -> "result");

        // assert
        assertThat(result).isEqualTo("result");
    }

    static List<BiFunction<NoTransactionSupport, Supplier<String>, String>> callingWork() {
        return List.of(NoTransactionSupport::callInNewTransaction, NoTransactionSupport::callInTransaction);
    }

    @ParameterizedTest
    @MethodSource("completions")
    void runsTheAfterCommitActionOnlyWhenCommitted(int status, List<String> expected) {
        // arrange
        AfterCommitSynchronization synchronization = new AfterCommitSynchronization(() -> runs.add("run"));
        synchronization.beforeCompletion();

        // act
        synchronization.afterCompletion(status);

        // assert
        assertThat(runs).isEqualTo(expected);
    }

    static List<Arguments> completions() {
        return List.of(Arguments.of(Status.STATUS_COMMITTED, List.of("run")),
                Arguments.of(Status.STATUS_ROLLEDBACK, List.of()));
    }

    @Test
    void listenerFailureCarriesTheCheckedException() {
        // arrange
        Exception cause = new Exception("checked");

        // act
        ListenerFailure failure = new ListenerFailure(cause);

        // assert
        assertThat(failure).hasCause(cause);
    }
}
