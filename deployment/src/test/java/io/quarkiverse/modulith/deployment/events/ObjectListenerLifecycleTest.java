package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.objects.AuditedLiteral;
import io.quarkiverse.modulith.deployment.fixtures.events.objects.ObjectListener;
import io.quarkiverse.modulith.deployment.fixtures.events.objects.Pong;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A listener of {@code Object} gets observers qualified {@code @Default}: every unqualified event reaches it, no
 * qualified one does, so ArC keeps skipping the context lifecycle events nobody else observes.
 */
public class ObjectListenerLifecycleTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Pong.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Pong.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Event<Pong> events;

    @Inject
    @Any
    Event<Pong> anyEvents;

    @Inject
    BeanManager beanManager;

    @BeforeEach
    void clear() {
        ObjectListener.RECEIVED.clear();
    }

    @Test
    void receivesUnqualifiedEventsAfterAContextLifecycle() {
        // arrange
        ManagedContext requestContext = Arc.container().requestContext();

        // act
        requestContext.activate();
        requestContext.terminate();
        events.fire(new Pong("p-1"));

        // assert
        assertThat(ObjectListener.RECEIVED).containsExactly(new Pong("p-1"));
    }

    @Test
    void ignoresQualifiedEvents() {
        // act
        anyEvents.select(AuditedLiteral.INSTANCE).fire(new Pong("p-2"));

        // assert
        assertThat(ObjectListener.RECEIVED).isEmpty();
    }

    @Test
    void observesNoContextLifecycleEvent() {
        // act
        var observers = beanManager.resolveObserverMethods(new Object(), Initialized.Literal.REQUEST);

        // assert
        assertThat(observers).extracting(ObserverMethod::getBeanClass).doesNotContain(ModuleEventDispatcher.class);
    }

    @Test
    void observesUnqualifiedEventsSynchronouslyAndAsynchronously() {
        // act
        var observers = beanManager.resolveObserverMethods(new Pong("p-3"), Default.Literal.INSTANCE);

        // assert
        assertThat(observers).filteredOn(it -> it.getBeanClass().equals(ModuleEventDispatcher.class))
                .extracting(ObserverMethod::isAsync).containsExactlyInAnyOrder(false, true);
    }
}
