package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.deployment.fixtures.events.observers.Important;
import io.quarkiverse.modulith.deployment.fixtures.events.observers.Marker;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.MarkedObserver;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.Pinned;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.PinnedObserver;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.RequestObserver;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.RoutingObservers;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.SessionObserver;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.Tenant;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.TenantObserver;
import io.quarkiverse.modulith.deployment.fixtures.events.routing.Trigger;

class RoutedObserversTest {

    static final Index INDEX = index();

    @Test
    void routesUnqualifiedAfterSuccessObserversOfServedBeans() {
        // act
        List<ListenerDeclaration> routed = ListenerDeclarations.routedObservers(INDEX, Optional.empty(), name -> true,
                Set.of());

        // assert
        assertThat(routed).extracting(it -> it.method().declaringClass().name().withoutPackagePrefix() + "#"
                + it.method().name()).containsExactlyInAnyOrder("RoutingObservers#routed", "RoutingObservers#always",
                        "RoutingObservers#marked", "RoutingObservers#triggered", "RequestObserver#on",
                        "MarkedObserver#on");
        assertThat(routed).allSatisfy(it -> {
            assertThat(it.routedObserver()).isTrue();
            assertThat(it.attributes()).isNull();
        });
    }

    @Test
    void keepsTheCdiDeliveryOfQualifiersRegisteredByExtensions() {
        // act
        List<ListenerDeclaration> routed = ListenerDeclarations.routedObservers(INDEX, Optional.empty(), name -> true,
                Set.of(DotName.createSimple(Marker.class)));

        // assert
        assertThat(routed).extracting(it -> it.method().name()).doesNotContain("marked");
    }

    @Test
    void routesOnlyObserversCarryingTheTrigger() {
        // act
        List<ListenerDeclaration> routed = ListenerDeclarations.routedObservers(INDEX,
                Optional.of(DotName.createSimple(Trigger.class)), name -> true, Set.of());

        // assert
        assertThat(routed).extracting(it -> it.method().name()).containsExactly("triggered");
    }

    @Test
    void neverRoutesObserversOfLibraries() {
        // act
        List<ListenerDeclaration> routed = ListenerDeclarations.routedObservers(INDEX, Optional.empty(),
                name -> !name.equals(DotName.createSimple(RoutingObservers.class)), Set.of());

        // assert
        assertThat(routed).extracting(it -> it.method().declaringClass().name().withoutPackagePrefix())
                .containsExactlyInAnyOrder("RequestObserver", "MarkedObserver");
    }

    @ParameterizedTest
    @CsvSource({ "ifExists, it is notified only if its bean instance exists (notifyObserver = IF_EXISTS)",
            "qualified, 'it is qualified, and the registry routes events by type only'" })
    void explainsWhyAnObserverKeepsItsCdiDelivery(String method, String reason) {
        // arrange
        var observes = INDEX.getClassByName(RoutingObservers.class).firstMethod(method).parameters().get(0)
                .declaredAnnotation(ListenerDeclarations.OBSERVES);

        // act
        Optional<String> kept = ListenerDeclarations.plainDeliveryReason(INDEX, observes, Set.of());

        // assert
        assertThat(kept).hasValue(reason);
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.deployment.fixtures.events.routing.SessionObserver, SessionScoped",
            "io.quarkiverse.modulith.deployment.fixtures.events.routing.TenantObserver, Tenant",
            "io.quarkiverse.modulith.deployment.fixtures.events.routing.PinnedObserver, Pinned",
            "io.quarkiverse.modulith.deployment.fixtures.events.routing.RequestObserver, ",
            "io.quarkiverse.modulith.deployment.fixtures.events.routing.MarkedObserver, ",
            "io.quarkiverse.modulith.deployment.fixtures.events.routing.RoutingObservers, " })
    void findsTheScopesTheDispatcherDoesNotServe(String bean, String expected) {
        // act
        Optional<DotName> scope = ListenerDeclarations.unservedScope(INDEX, INDEX.getClassByName(bean));

        // assert
        assertThat(scope.map(DotName::withoutPackagePrefix)).isEqualTo(Optional.ofNullable(expected));
    }

    private static Index index() {
        try {
            return Index.of(RoutingObservers.class, SessionObserver.class, TenantObserver.class, Tenant.class,
                    RequestObserver.class, MarkedObserver.class, Trigger.class, Important.class, Marker.class,
                    PinnedObserver.class, Pinned.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
