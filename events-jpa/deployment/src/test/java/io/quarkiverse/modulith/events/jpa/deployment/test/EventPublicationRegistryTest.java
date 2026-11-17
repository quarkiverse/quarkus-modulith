package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.jpa.JpaEventPublication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal.InvoiceListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.tck.Statements;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class EventPublicationRegistryTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addClass(Statements.class))
            .withConfigurationResource("application-h2.properties");

    @Inject
    OrderService orders;

    @Inject
    IncompleteEventPublications incomplete;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void reset() {
        QuarkusTransaction.requiringNew()
                .run(() -> Statements.deleteAll(entityManager, "JpaEventPublication"));
        InvoiceListener.INVOICED.clear();
        InvoiceListener.FAILURES_TO_SIMULATE.set(0);
    }

    @Test
    void marksPublicationCompletedWhenListenerSucceeds() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1", 100);

        // act
        orders.place(event.orderId(), event.amountInCents());
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(InvoiceListener.INVOICED).containsExactly(event);
        assertThat(incomplete.findIncompletePublications()).isEmpty();
        assertThat(completed.findAll()).singleElement().satisfies(p -> {
            assertThat(p.getListenerId()).isEqualTo("invoice.on-order-placed");
            assertThat(p.getEvent()).isEqualTo(event);
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
            assertThat(p.getCompletionDate()).isPresent();
            assertThat(p.getLastResubmissionDate()).isEmpty();
        });
    }

    @Test
    void keepsFailedPublicationAndCompletesItWhenResubmitted() {
        // arrange
        InvoiceListener.FAILURES_TO_SIMULATE.set(1);
        orders.place("o-2", 200);
        dispatcher.awaitIdle(Duration.ofSeconds(10));
        List<EventPublication> failed = incomplete.findIncompletePublications();

        // act
        int resubmitted = incomplete.resubmitIncompletePublications(p -> true);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(failed).singleElement().satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
            assertThat(p.getEvent()).isEqualTo(new OrderPlaced("o-2", 200));
        });
        assertThat(resubmitted).isEqualTo(1);
        assertThat(idle).isTrue();
        assertThat(InvoiceListener.INVOICED).containsExactly(new OrderPlaced("o-2", 200));
        assertThat(incomplete.findIncompletePublications()).isEmpty();
        assertThat(completed.findAll()).singleElement().satisfies(p -> {
            assertThat(p.getCompletionAttempts()).isEqualTo(2);
            assertThat(p.getLastResubmissionDate()).isPresent();
            assertThat(p.getLastPublicationDate()).isEqualTo(p.getLastResubmissionDate().get());
        });
    }

    @Test
    void storesNothingWhenThePublishingTransactionRollsBack() {
        // arrange
        String order = "o-3";

        // act
        Throwable failure = catchThrowable(() -> orders.placeAndRollBack(order));
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(failure).hasMessageContaining("rolled back o-3");
        assertThat(idle).isTrue();
        assertThat(incomplete.findIncompletePublications()).isEmpty();
        assertThat(completed.findAll()).isEmpty();
        assertThat(InvoiceListener.INVOICED).isEmpty();
    }

    @Test
    void deletesCompletedPublicationsMatchingAPredicate() {
        // arrange
        orders.place("o-5", 500);
        orders.place("o-6", 600);
        dispatcher.awaitIdle(Duration.ofSeconds(10));

        // act
        int matching = completed.deletePublications(p -> p.getEvent().equals(new OrderPlaced("o-5", 500)));
        int none = completed.deletePublications(p -> false);

        // assert
        assertThat(matching).isEqualTo(1);
        assertThat(none).isZero();
        assertThat(completed.findAll()).extracting(EventPublication::getEvent).containsExactly(new OrderPlaced("o-6", 600));
    }

    @Test
    void marksPublicationsWhoseListenerIsGoneFailed() {
        // arrange
        insert("gone.listener", OrderPlaced.class.getName(), "{\"orderId\":\"o-7\",\"amountInCents\":7}");
        List<EventPublication> before = incomplete.findIncompletePublications();

        // act
        int resubmitted = incomplete.resubmitIncompletePublications(p -> true);

        // assert
        assertThat(before).singleElement().satisfies(p -> {
            assertThat(p.getCompletionAttempts()).isZero();
            assertThat(p.getEvent()).isEqualTo(new OrderPlaced("o-7", 7));
        });
        assertThat(resubmitted).isZero();
        assertThat(incomplete.findIncompletePublications()).extracting(EventPublication::getStatus)
                .containsExactly(Status.FAILED);
    }

    @Test
    void failsToReadPublicationsOfUnknownTypes() {
        // arrange
        insert("invoice.on-order-placed", "com.acme.Removed", "{}");
        EventPublication publication = incomplete.findIncompletePublications().get(0);

        // act & assert
        assertThatThrownBy(publication::getEvent)
                .hasRootCauseInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("of type com.acme.Removed");
    }

    @Test
    void deletesCompletedPublications() {
        // arrange
        orders.place("o-4", 400);
        dispatcher.awaitIdle(Duration.ofSeconds(10));

        // act
        int deleted = completed.deletePublicationsOlderThan(Duration.ofSeconds(-1));

        // assert
        assertThat(deleted).isEqualTo(1);
        assertThat(completed.findAll()).isEmpty();
    }

    /**
     * Inserts a row the way an older version, or Spring Modulith, may have left it: no attempt count.
     */
    void insert(String listenerId, String eventType, String json) {
        QuarkusTransaction.requiringNew().run(() -> {
            JpaEventPublication row = new JpaEventPublication();
            row.id = UUID.randomUUID();
            row.listenerId = listenerId;
            row.eventType = eventType;
            row.serializedEvent = json;
            row.publicationDate = Instant.now();
            row.status = Status.PUBLISHED;
            entityManager.persist(row);
        });
    }
}
