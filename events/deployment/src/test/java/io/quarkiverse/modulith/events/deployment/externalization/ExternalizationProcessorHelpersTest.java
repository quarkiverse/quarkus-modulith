package io.quarkiverse.modulith.events.deployment.externalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport.CustomTransport;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport.OtherProducers;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport.TransportField;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.transport.TransportMethod;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

class ExternalizationProcessorHelpersTest {

    @Test
    void detectsTransportsOfTheApplication() throws IOException {
        // act
        boolean implementation = ExternalizationProcessor.hasApplicationTransport(
                Index.of(EventExternalizationTransport.class, CustomTransport.class));
        boolean method = ExternalizationProcessor.hasApplicationTransport(Index.of(TransportMethod.class));
        boolean field = ExternalizationProcessor.hasApplicationTransport(Index.of(TransportField.class));
        boolean otherProducers = ExternalizationProcessor.hasApplicationTransport(Index.of(OtherProducers.class));
        boolean nothing = ExternalizationProcessor.hasApplicationTransport(Index.of(new Class<?>[0]));

        // assert
        assertThat(implementation).isTrue();
        assertThat(method).isTrue();
        assertThat(field).isTrue();
        assertThat(otherProducers).isFalse();
        assertThat(nothing).isFalse();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "com.acme.order.OrderPlaced|''|com.acme|order.OrderPlaced",
            "com.acme.OrderPlaced|''|com.acme|OrderPlaced",
            "com.acme.order.OrderPlaced|::{event.id}|com.acme|order.OrderPlaced",
            "com.acmeorder.OrderPlaced|''|com.acme|com.acmeorder.OrderPlaced",
            "com.acme.order.OrderPlaced|''|null|com.acme.order.OrderPlaced",
            "OrderPlaced|''|com.acme|OrderPlaced",
            "com.acme.order.OrderPlaced|orders::{event.id}|com.acme|orders",
            "com.acme.order.OrderPlaced|#{#this.topic()}|com.acme|null",
            "com.acme.order.OrderPlaced|{event.topic}::key|com.acme|null"
    })
    void computesTheStaticTarget(String type, String declaration, String basePackage, String expected) {
        // act
        Optional<String> target = ExternalizationProcessor.staticTarget(type, declaration,
                Optional.ofNullable(basePackage));

        // assert
        assertThat(target.orElse(null)).isEqualTo(expected);
    }

    @Test
    void collectsTheExpressionParts() {
        // act
        Set<String> expressions = ExternalizationProcessor.expressions(Set.of("orders::{event.id}",
                "#{#this.topic()}::#{#this.key()}", "plain::key", "", "::{event.other}"));

        // assert
        assertThat(expressions).containsExactly("#{#this.key()}", "#{#this.topic()}", "{event.id}", "{event.other}");
    }
}
