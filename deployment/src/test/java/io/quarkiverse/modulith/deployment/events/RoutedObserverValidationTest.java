package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.Failures;
import io.quarkiverse.modulith.deployment.fixtures.events.routed.Shipment;
import io.quarkiverse.modulith.deployment.fixtures.events.routed.Tracked;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Stands in for an event publication registry with a build step producing {@link RegistryRoutingBuildItem}, and for an
 * extension registering {@link Tracked} as a qualifier: the static observer of a tracked event is not routed.
 */
public class RoutedObserverValidationTest {

    static final String OBSERVERS = "io.quarkiverse.modulith.deployment.fixtures.events.routed.RoutedObservers#";
    static final String FIX = " gets event publications from the event publication registry, which cannot call a static"
            + " or private method: make it an instance method that is not private, or set"
            + " quarkus.modulith.events.registry-trigger-annotation to route only annotated observers";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Shipment.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Shipment.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .addBuildChainCustomizer(chain -> chain
                    .addBuildStep(context -> context.produce(new RegistryRoutingBuildItem(Optional.empty())))
                    .produces(RegistryRoutingBuildItem.class).build())
            .addBuildChainCustomizer(chain -> chain
                    .addBuildStep(context -> context.produce(new QualifierRegistrarBuildItem(
                            () -> Map.of(DotName.createSimple(Tracked.class), Set.of()))))
                    .produces(QualifierRegistrarBuildItem.class).build())
            .assertException(t -> assertThat(Failures.messages(t))
                    .contains("The AFTER_SUCCESS observer " + OBSERVERS + "onStatic" + FIX)
                    .contains("The AFTER_SUCCESS observer " + OBSERVERS + "onPrivate" + FIX)
                    .doesNotContain("@ApplicationModuleListener", "onTracked"));

    @Test
    void namesTheObserversAndTheFix() {
    }
}
