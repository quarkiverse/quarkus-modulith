package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkiverse.modulith.deployment.fixtures.valid.inventory.internal.Received;
import io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderCompleted;
import io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderManagement;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    OrderManagement orders;

    @Inject
    Received received;

    @Inject
    ModuleEventDispatcher dispatcher;

    @BeforeEach
    void reset() {
        received.clear();
    }

    @Test
    void deliversAfterCommitOnWorkerThreadInNewTransaction() {
        // act
        orders.complete("o-1");

        // assert
        assertThat(dispatcher.awaitIdle(Duration.ofSeconds(10))).isTrue();
        assertThat(received.events()).containsExactly(new OrderCompleted("o-1"));
        assertThat(received.threads()).doesNotContain(Thread.currentThread().getName());
        assertThat(received.inTransaction()).containsExactly(true);
    }

    @Test
    void dropsEventsOfRolledBackTransactions() {
        // act & assert
        assertThatThrownBy(() -> orders.completeAndFail("o-2")).hasMessageContaining("rollback o-2");

        // assert
        assertThat(dispatcher.awaitIdle(Duration.ofSeconds(10))).isTrue();
        assertThat(received.events()).isEmpty();
    }

    @Test
    void deliversImmediatelyWithoutTransaction() {
        // act
        orders.completeWithoutTransaction("o-3");

        // assert
        assertThat(dispatcher.awaitIdle(Duration.ofSeconds(10))).isTrue();
        assertThat(received.events()).containsExactly(new OrderCompleted("o-3"));
    }

    @Test
    void registersListenerWithDefaultId() {
        // act
        var ids = dispatcher.listenerIds();

        // assert
        assertThat(ids).containsExactly(
                "io.quarkiverse.modulith.deployment.fixtures.valid.inventory.internal.InventoryListener.on("
                        + "io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderCompleted)");
    }
}
