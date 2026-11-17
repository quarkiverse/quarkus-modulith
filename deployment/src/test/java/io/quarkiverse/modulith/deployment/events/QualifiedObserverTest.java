package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;

import org.jboss.jandex.Index;
import org.jboss.jandex.MethodParameterInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.deployment.fixtures.events.observers.Important;
import io.quarkiverse.modulith.deployment.fixtures.events.observers.Marker;
import io.quarkiverse.modulith.deployment.fixtures.events.observers.Observers;

class QualifiedObserverTest {

    @ParameterizedTest
    @CsvSource({ "plain, false", "prioritized, false", "marked, false", "important, true", "named, true" })
    void treatsQualifiersAndUnknownAnnotationsAsQualifiers(String method, boolean expected) throws IOException {
        // arrange
        Index index = Index.of(Observers.class, Marker.class, Important.class);
        MethodParameterInfo parameter = index.getClassByName(Observers.class).firstMethod(method).parameters().get(0);

        // act
        boolean qualified = ListenerDeclarations.qualified(index, parameter, Set.of());

        // assert
        assertThat(qualified).isEqualTo(expected);
    }
}
