package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.event.Event;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.observe.Child;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.observe.Parent;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.observe.Ping;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.observe.Signal;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.observe.Unrelated;

class ObservedEventTypesTest {

    private static final String APP_BEAN = "com.acme.Service";

    private static EventInjectionPoint fires(Type type) {
        return new EventInjectionPoint(ParameterizedType.create(Event.class, type), APP_BEAN);
    }

    private static Type type(Class<?> type) {
        return ClassType.create(type);
    }

    private static ObservedEventTypes observed() throws IOException {
        return new ObservedEventTypes(Index.of(Parent.class, Child.class, Signal.class, Ping.class,
                Unrelated.class), APP_BEAN::equals);
    }

    static Stream<Arguments> keepsOnlyTheTopmostTypes() {
        return Stream.of(
                Arguments.of(List.of(fires(type(Child.class)), fires(type(Parent.class))), List.of(),
                        Set.of(DotName.createSimple(Parent.class))),
                Arguments.of(List.of(fires(type(Signal.class))), List.of(Ping.class.getName(), Unrelated.class.getName()),
                        Set.of(DotName.createSimple(Signal.class), DotName.createSimple(Unrelated.class))),
                Arguments.of(List.of(fires(type(Child.class)), fires(type(Object.class))), List.of(),
                        Set.of(ObservedEventTypes.OBJECT)),
                Arguments.of(List.of(new EventInjectionPoint(ClassType.create(Event.class), APP_BEAN)), List.of(),
                        Set.of(ObservedEventTypes.OBJECT)),
                Arguments.of(List.of(fires(type(Ping.class)),
                        new EventInjectionPoint(type(String.class), APP_BEAN)), List.of(),
                        Set.of(DotName.createSimple(Ping.class))),
                Arguments.of(List.of(new EventInjectionPoint(ParameterizedType.create(Event.class, type(Object.class)),
                        "org.library.Publisher"),
                        fires(type(Child.class))), List.of(), Set.of(DotName.createSimple(Child.class))),
                Arguments.of(List.of(), List.of(), Set.of()));
    }

    @ParameterizedTest
    @MethodSource
    void keepsOnlyTheTopmostTypes(List<EventInjectionPoint> points, List<String> externalized, Set<DotName> expected)
            throws IOException {
        // act
        Set<DotName> types = observed().collect(points, externalized);

        // assert
        assertThat(types).containsExactlyInAnyOrderElementsOf(expected);
    }

    static Stream<Arguments> readsTheFiredType() {
        return Stream.of(
                Arguments.of(ParameterizedType.create(Event.class, type(Ping.class)), DotName.createSimple(Ping.class)),
                Arguments.of(ParameterizedType.create(Event.class, ParameterizedType.create(List.class, type(String.class))),
                        DotName.createSimple(List.class)),
                Arguments.of(ParameterizedType.create(Event.class, TypeVariable.create("T")), ObservedEventTypes.OBJECT),
                Arguments.of(ParameterizedType.create(Event.class, WildcardType.UNBOUNDED), ObservedEventTypes.OBJECT),
                Arguments.of(ClassType.create(Event.class), ObservedEventTypes.OBJECT));
    }

    @ParameterizedTest
    @MethodSource
    void readsTheFiredType(Type event, DotName expected) {
        // act
        DotName fired = ObservedEventTypes.eventType(event);

        // assert
        assertThat(fired).isEqualTo(expected);
    }

    @Test
    void walksSupertypesKnownToTheIndex() throws IOException {
        // act
        Set<DotName> child = observed().supertypes(DotName.createSimple(Child.class));
        Set<DotName> ping = observed().supertypes(DotName.createSimple(Ping.class));
        Set<DotName> unknown = observed().supertypes(DotName.createSimple("com.acme.Unknown"));

        // assert
        assertThat(child).containsExactly(DotName.createSimple(Parent.class));
        assertThat(ping).containsExactly(DotName.createSimple(Signal.class));
        assertThat(unknown).isEmpty();
    }
}
