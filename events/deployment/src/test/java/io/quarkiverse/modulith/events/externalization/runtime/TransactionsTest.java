package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.externalization.fakes.FakeRegistry;

class TransactionsTest {

    private final List<String> work = new ArrayList<>();
    private final FakeRegistry registry = new FakeRegistry();
    private final JtaExternalizationTransactions jta = new JtaExternalizationTransactions();

    TransactionsTest() {
        jta.registry = registry;
    }

    @Test
    void runsWorkRightAwayWithoutTransactionManager() {
        // arrange
        NoExternalizationTransactions none = new NoExternalizationTransactions();

        // act
        none.afterCommit(() -> work.add("done"));

        // assert
        assertThat(none.isActive()).isFalse();
        assertThat(work).containsExactly("done");
    }

    @Test
    void runsWorkRightAwayOutsideTransactions() {
        // act
        jta.afterCommit(() -> work.add("done"));

        // assert
        assertThat(jta.isActive()).isFalse();
        assertThat(work).containsExactly("done");
        assertThat(registry.synchronizations()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "3,true", "4,false", "1,false" })
    void runsWorkOnlyAfterTheCommit(int status, boolean ran) {
        // arrange
        registry.status(Status.STATUS_ACTIVE);
        jta.afterCommit(() -> work.add("done"));

        // act
        registry.synchronizations().forEach(synchronization -> {
            synchronization.beforeCompletion();
            synchronization.afterCompletion(status);
        });

        // assert
        assertThat(jta.isActive()).isTrue();
        assertThat(registry.synchronizations()).hasSize(1);
        assertThat(work).hasSize(ran ? 1 : 0);
    }
}
