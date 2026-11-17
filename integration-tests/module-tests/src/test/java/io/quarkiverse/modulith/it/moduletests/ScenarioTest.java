package io.quarkiverse.modulith.it.moduletests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.transaction.Status;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.it.moduletests.audit.AuditLog;
import io.quarkiverse.modulith.it.moduletests.inventory.Inventory;
import io.quarkiverse.modulith.it.moduletests.inventory.StockReduced;
import io.quarkiverse.modulith.it.moduletests.order.OrderCancelled;
import io.quarkiverse.modulith.it.moduletests.order.OrderPlaced;
import io.quarkiverse.modulith.it.moduletests.order.Orders;
import io.quarkiverse.modulith.test.EnableScenarios;
import io.quarkiverse.modulith.test.Scenario;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@EnableScenarios
class ScenarioTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    @Inject
    Inventory inventory;

    @Inject
    Orders orders;

    @Inject
    AuditLog audit;

    @Test
    void publishWaitsForTheStateChange(Scenario scenario) {
        // arrange
        AtomicReference<Integer> verified = new AtomicReference<>();

        // act
        scenario.publish(new OrderPlaced("o-p1", "sku-p1", 3))
                .andWaitAtMost(WAIT)
                .andWaitForStateChange(() -> inventory.stockOf("sku-p1"), stock -> stock == 97)
                .andVerify(verified::set);

        // assert
        assertThat(verified.get()).isEqualTo(97);
    }

    @Test
    void publishFromSupplierWaitsForTheFollowUpEvent(Scenario scenario) {
        // arrange
        AtomicReference<StockReduced> verified = new AtomicReference<>();

        // act
        scenario.publish(() -> new OrderPlaced("o-p2", "sku-p2", 2))
                .andWaitAtMost(WAIT)
                .forEventOfType(StockReduced.class)
                .matching(event -> event.sku().equals("sku-p2"))
                .toArriveAndVerify(verified::set);

        // assert
        assertThat(verified.get()).isEqualTo(new StockReduced("sku-p2", 98));
    }

    @Test
    void stimulateRunnableWaitsForAMatchingEvent(Scenario scenario) {
        // act
        OrderPlaced event = scenario.stimulate(() -> {
            orders.place("sku-r1", 1);
        }).andWaitForEventOfType(OrderPlaced.class)
                .matchingMapped(OrderPlaced::sku, "sku-r1"::equals)
                .toArrive();

        // assert
        assertThat(event.quantity()).isEqualTo(1);
    }

    @Test
    void stimulateSupplierPassesItsResultToTheVerification(Scenario scenario) {
        // arrange
        List<String> verified = new ArrayList<>();
        AtomicReference<String> placed = new AtomicReference<>();

        // act
        scenario.stimulate(() -> {
            String orderId = orders.place("sku-s1", 2);
            placed.set(orderId);
            return orderId;
        })
                .forEventOfType(OrderPlaced.class)
                .matchingMappedValue(OrderPlaced::sku, "sku-s1")
                .toArriveAndVerify((event, orderId) -> {
                    verified.add(event.orderId());
                    verified.add(orderId);
                });

        // assert
        assertThat(verified).containsExactly(placed.get(), placed.get());
    }

    @Test
    void stimulateSupplierRunsInANewTransaction(Scenario scenario) {
        // arrange
        AtomicReference<Boolean> active = new AtomicReference<>();

        // act
        scenario.stimulate(() -> QuarkusTransaction.getStatus() == Status.STATUS_ACTIVE)
                .forStateChange(() -> "done")
                .andVerify((state, transactional) -> active.set(transactional));

        // assert
        assertThat(active.get()).isTrue();
    }

    @Test
    void stimulateWithTransactionOperationsAssertsTheEvent(Scenario scenario) {
        // arrange
        AtomicReference<String> placed = new AtomicReference<>();

        // act
        scenario.stimulate(tx -> tx.execute(() -> orders.place("sku-f1", 4)))
                .andWaitForEventOfType(OrderPlaced.class)
                .matchingMappedValue(OrderPlaced::sku, "sku-f1")
                .toArriveAndAssert((events, orderId) -> {
                    events.matching(event -> event.equals(new OrderPlaced(orderId, "sku-f1", 4)));
                    placed.set(orderId);
                });

        // assert
        assertThat(placed.get()).startsWith("o-");
    }

    @Test
    void stimulateWithPublisherAssertsTheEvent(Scenario scenario) {
        // arrange
        List<String> asserted = new ArrayList<>();

        // act
        scenario.stimulate((tx, publisher) -> {
            tx.executeWithoutResult(() -> publisher.fire(new OrderCancelled("o-c1")));
        }).andWaitForEventOfType(OrderCancelled.class)
                .toArriveAndAssert(events -> {
                    events.matching(event -> event.equals(new OrderCancelled("o-c1")));
                    asserted.add("o-c1");
                });

        // assert
        assertThat(asserted).containsExactly("o-c1");
    }

    @Test
    void stimulateWithPublisherReturnsAResult(Scenario scenario) {
        // arrange
        List<String> verified = new ArrayList<>();

        // act
        scenario.stimulate((tx, publisher) -> {
            publisher.fire(new OrderCancelled("o-c2"));
            return "o-c2";
        }).andWaitForEventOfType(OrderCancelled.class)
                .toArriveAndVerify((event, result) -> verified.add(event.orderId() + "/" + result));

        // assert
        assertThat(verified).containsExactly("o-c2/o-c2");
    }

    @Test
    void cleanupRunsAfterAFailedWait(Scenario scenario) {
        // arrange
        List<String> cleaned = new ArrayList<>();

        // act
        Throwable failure = catchThrowable(() -> scenario
                .stimulate(() -> "result")
                .andCleanup(cleaned::add)
                .customize(it -> it.atMost(Duration.ofMillis(200)))
                .andWaitForStateChange(() -> false));

        // assert
        assertThat(failure).isInstanceOf(ConditionTimeoutException.class).hasMessageContaining("200 milliseconds");
        assertThat(cleaned).containsExactly("result");
    }

    @Test
    void cleanupRunnableRunsAfterTheScenario(Scenario scenario) {
        // arrange
        List<String> cleaned = new ArrayList<>();

        // act
        scenario.stimulate(() -> audit.record("cleanup"))
                .andCleanup(() -> cleaned.add("cleaned"))
                .forStateChange(audit::entries)
                .andVerify(entries -> assertThat(entries).contains("cleanup"));

        // assert
        assertThat(cleaned).containsExactly("cleaned");
    }

    @Test
    void verifiesThePublishedEventsAfterAStateChange(Scenario scenario) {
        // arrange
        List<String> verified = new ArrayList<>();

        // act
        scenario.publish(new OrderPlaced("o-v1", "sku-v1", 1))
                .andWaitAtMost(WAIT)
                .forStateChange(() -> inventory.stockOf("sku-v1"), stock -> stock == 99)
                .andVerifyEvents(events -> {
                    assertThat(events).contains(OrderPlaced.class).matchingValue(OrderPlaced::sku, "sku-v1");
                    verified.add("verified");
                });

        // assert
        assertThat(verified).containsExactly("verified");
    }

    @Test
    void expectedEventMustHaveBeenPublished(Scenario scenario) {
        // arrange
        var result = scenario.publish(new OrderPlaced("o-x1", "sku-x1", 1))
                .andWaitAtMost(WAIT)
                .andWaitForStateChange(() -> inventory.stockOf("sku-x1"), stock -> stock == 99)
                .andExpect(OrderCancelled.class);

        // act
        Throwable failure = catchThrowable(result::toArrive);

        // assert
        assertThat(failure).isInstanceOf(AssertionError.class).hasMessageStartingWith(
                "Expected an event of type io.quarkiverse.modulith.it.moduletests.order.OrderCancelled");
    }

    @Test
    void expectedEventIsVerifiedWithTheStimulusResult(Scenario scenario) {
        // arrange
        List<String> verified = new ArrayList<>();

        // act
        scenario.stimulate(() -> orders.place("sku-e1", 1))
                .andWaitAtMost(WAIT)
                .andWaitForStateChange(() -> inventory.stockOf("sku-e1"), stock -> stock == 99)
                .andExpect(OrderPlaced.class)
                .matchingMappedValue(OrderPlaced::sku, "sku-e1")
                .toArriveAndVerify((event, orderId) -> verified.add(String.valueOf(event.orderId().equals(orderId))));

        // assert
        assertThat(verified).containsExactly("true");
    }

    @Test
    void waitingForAnEventTimesOut(Scenario scenario) {
        // arrange
        var result = scenario.stimulate(() -> {
        }).andWaitAtMost(Duration.ofMillis(150)).andWaitForEventOfType(OrderCancelled.class);

        // act
        Throwable failure = catchThrowable(result::toArrive);

        // assert
        assertThat(failure).isInstanceOf(ConditionTimeoutException.class).hasMessageContaining("150 milliseconds");
    }
}
