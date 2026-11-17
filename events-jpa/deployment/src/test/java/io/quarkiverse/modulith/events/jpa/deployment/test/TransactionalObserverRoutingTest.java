package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.stream.Stream;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.library.LibraryEvent;
import io.quarkiverse.modulith.events.jpa.deployment.library.LibraryObserver;
import io.quarkiverse.modulith.events.jpa.deployment.routed.RoutedObservers;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class TransactionalObserverRoutingTest {

    static final String ROUTED = RoutedObservers.class.getName();
    static final String EVENT = "(io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced)";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addPackage(RoutedObservers.class.getPackage()))
            .withAdditionalDependency(jar -> jar.addClasses(LibraryObserver.class, LibraryEvent.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Inject
    IncompleteEventPublications incomplete;

    @Inject
    Event<String> triggers;

    @Test
    void storesAndCompletesPublicationsOfAfterSuccessObservers() {
        // arrange
        RoutedObservers.CALLS.clear();

        // act
        orders.place("t-1", 1);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(RoutedObservers.CALLS).containsExactlyInAnyOrder("in-progress:t-1", "after-completion:t-1",
                "after-success:t-1",
                "marked:t-1", "with-bean-manager:t-1", "prioritized:t-1");
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId).containsExactlyInAnyOrder(
                "invoice.on-order-placed", ROUTED + ".afterSuccess" + EVENT, ROUTED + ".marked" + EVENT,
                ROUTED + ".prioritized" + EVENT);
        assertThat(Arc.container().beanManager().getBeans(Object.class, Any.Literal.INSTANCE))
                .extracting(bean -> bean.getBeanClass().getName()).contains(LibraryObserver.class.getName());
    }

    @Test
    void leavesObserversOfLibrariesToPlainCdi() {
        // arrange
        System.clearProperty(LibraryObserver.CALLS);

        // act
        QuarkusTransaction.requiringNew().run(() -> triggers.select(NamedLiteral.of(LibraryObserver.TRIGGER)).fire("l-1"));

        // assert
        assertThat(System.clearProperty(LibraryObserver.CALLS)).isEqualTo("library:l-1;");
        assertThat(Stream.concat(incomplete.findIncompletePublications().stream(), completed.findAll().stream()))
                .extracting(EventPublication::getListenerId)
                .noneMatch(listener -> listener.startsWith(LibraryObserver.class.getName()));
    }
}
