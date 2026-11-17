package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.readonly.ReadOnlyListener;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class ReadOnlyArchiveModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addPackage(ReadOnlyListener.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "archive")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    IncompleteEventPublications incomplete;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void archivesTheCompletionOfAReadOnlyListener() {
        // arrange
        ReadOnlyListener.SEEN.clear();

        // act
        orders.place("ra-1", 10);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(ReadOnlyListener.SEEN).containsExactly("read-only=true,manual=true");
        assertThat(incomplete.findIncompletePublications()).isEmpty();
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId)
                .containsExactlyInAnyOrder("read-only", "invoice.on-order-placed");
    }
}
