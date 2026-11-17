package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.deployment.fixtures.events.nomodel.Deliveries;
import io.quarkiverse.modulith.deployment.fixtures.events.nomodel.Other;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Application classes in the default package and in a named package share no package, so there is no module model.
 * Listeners still work and belong to no module, and module test settings are ignored.
 */
public class ListenerOutsideModelTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass("DefaultPackageListener").addClasses(Other.class, Deliveries.class))
            .overrideConfigKey("quarkus.modulith.test.module", "ignored without a model");

    @Inject
    ApplicationModules modules;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Inject
    Event<Other> events;

    @BeforeEach
    void clear() {
        Deliveries.RECEIVED.clear();
    }

    @Test
    void buildsNoModel() {
        // act
        String basePackage = modules.getBasePackage();

        // assert
        assertThat(basePackage).isEmpty();
        assertThat(modules.stream()).isEmpty();
    }

    @Test
    void registersTheListenerWithoutModule() {
        // act
        var ids = dispatcher.listenerIds();

        // assert
        assertThat(ids).containsExactly("DefaultPackageListener.on(" + Other.class.getName() + ")");
    }

    @Test
    void deliversInTheRequestContextOfThePublishingThread() {
        // arrange
        ManagedContext requestContext = Arc.container().requestContext();
        Other event = new Other();
        requestContext.activate();

        // act
        try {
            events.fire(event);
        } finally {
            requestContext.terminate();
        }

        // assert
        assertThat(Deliveries.RECEIVED).containsExactly(event);
    }
}
