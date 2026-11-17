package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.fakes.routing.Square;

class RoutingResolverTest {

    private final RoutingResolver resolver = new RoutingResolver();

    RoutingResolverTest() {
        resolver.expressions = new TestExpressions();
    }

    @Test
    void keepsTargetsWithoutExpressions() {
        // arrange
        RoutingTarget target = RoutingTarget.forTarget("plain").andKey("key");

        // act
        RoutingTarget resolved = resolver.resolve(target, new Square("s-1"));

        // assert
        assertThat(resolved).isSameAs(target);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{event.topic}|shapes",
            "{event.topic}::fixed|shapes::fixed",
            "shapes::{event.id}|shapes::s-1",
            "{event.topic}::{event.id}|shapes::s-1",
            "shapes::{event.none}|shapes"
    })
    void evaluatesExpressionsDeclaredOnSupertypes(String declared, String resolved) {
        // act
        RoutingTarget target = resolver.resolve(RoutingTarget.parse(declared), new Square("s-1"));

        // assert
        assertThat(target).isEqualTo(RoutingTarget.parse(resolved));
    }

    @ParameterizedTest
    @CsvSource({ "{event.none}", "{event.blank}" })
    void rejectsTargetsThatEvaluateToNothing(String declared) {
        // act & assert
        assertThatThrownBy(() -> resolver.resolve(RoutingTarget.forTarget(declared), new Square("s-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Evaluation of target expression " + declared
                        + " must not result in null or blank for an event of type " + Square.class.getName());
    }

    @Test
    void rejectsExpressionsTheBuildDidNotCompile() {
        // act & assert
        assertThatThrownBy(() -> resolver.resolve(RoutingTarget.forTarget("{event.unknown}"), new Square("s-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Routing expression {event.unknown} for an event of type " + Square.class.getName()
                        + " was not compiled at build time. Only expressions declared in @Externalized annotations"
                        + " are compiled");
    }

    @Test
    void walksTheHierarchyOncePerClassAndExpression() {
        // arrange
        RoutingTarget declared = RoutingTarget.parse("{event.topic}::{event.id}");

        // act
        RoutingTarget first = resolver.resolve(declared, new Square("s-1"));
        RoutingTarget second = resolver.resolve(declared, new Square("s-2"));

        // assert
        assertThat(first).isEqualTo(RoutingTarget.parse("shapes::s-1"));
        assertThat(second).isEqualTo(RoutingTarget.parse("shapes::s-2"));
        assertThat(resolver.found).containsOnlyKeys(Square.class);
        assertThat(resolver.found.get(Square.class)).containsOnlyKeys("{event.topic}", "{event.id}");
    }
}
