package io.quarkiverse.modulith.events.externalization.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.externalization.fakes.FakeCreationalContext;
import io.quarkiverse.modulith.events.externalization.fakes.FixedConfig;
import io.quarkiverse.modulith.events.externalization.fakes.routing.Square;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;
import io.quarkiverse.modulith.events.spi.EventRedelivery;

class CreatorsTest {

    @Test
    void createsTheDefaultConfigurationForTheBasePackages() {
        // arrange
        FakeCreationalContext<EventExternalizationConfiguration> context = new FakeCreationalContext<>(
                Map.of(DefaultConfigurationCreator.PACKAGES, new String[] { "io.quarkiverse.modulith.events" }),
                Map.of(ExternalizationRuntimeConfig.class, new FixedConfig(true, Duration.ofSeconds(1))));

        // act
        EventExternalizationConfiguration configuration = new DefaultConfigurationCreator().create(context);

        // assert
        assertThat(configuration.serializeExternalization()).isTrue();
        assertThat(configuration.supports(new Square("s"))).isFalse();
        assertThat(configuration.determineTarget(new Square("s")))
                .isEqualTo(RoutingTarget.forTarget("externalization.fakes.routing.Square"));
    }

    @Test
    void createsAConfigurationWithoutBasePackage() {
        // arrange
        FakeCreationalContext<EventExternalizationConfiguration> context = new FakeCreationalContext<>(
                Map.of(DefaultConfigurationCreator.PACKAGES, new String[0]),
                Map.of(ExternalizationRuntimeConfig.class, new FixedConfig(false, Duration.ofSeconds(1))));

        // act
        EventExternalizationConfiguration configuration = new DefaultConfigurationCreator().create(context);

        // assert
        assertThat(configuration.serializeExternalization()).isFalse();
        assertThat(configuration.supports(new Square("s"))).isFalse();
        assertThat(configuration.determineTarget(new Square("s")))
                .isEqualTo(RoutingTarget.forTarget(Square.class.getName()));
    }

    @Test
    void createsTheRedeliveryOfTheStrategy() {
        // arrange
        List<UUID> resubmitted = new ArrayList<>();
        UUID id = UUID.randomUUID();
        ExternalizationStrategy strategy = new ResubmittingStrategy(resubmitted);
        FakeCreationalContext<EventRedelivery> context = new FakeCreationalContext<>(Map.of(),
                Map.of(ExternalizationStrategy.class, strategy));

        // act
        EventRedelivery redelivery = new ExternalizationRedeliveryCreator().create(context);

        // assert
        assertThat(redelivery.redeliver(id, "listener", "event")).isTrue();
        assertThat(resubmitted).containsExactly(id);
    }

    @Test
    void findsCompiledExpressionsByDeclaringType() {
        // arrange
        CompiledRoutingExpressions expressions = new EmptyExpressions();
        expressions.register("com.acme.Event", "{event.id}", event -> "id");

        // act
        Optional<RoutingExpression> found = expressions.find("com.acme.Event", "{event.id}");
        Optional<RoutingExpression> otherExpression = expressions.find("com.acme.Event", "{event.other}");
        Optional<RoutingExpression> otherType = expressions.find("com.acme.Other", "{event.id}");

        // assert
        assertThat(found).get().satisfies(expression -> assertThat(expression.evaluate("e")).isEqualTo("id"));
        assertThat(otherExpression).isEmpty();
        assertThat(otherType).isEmpty();
    }

    @Test
    void freezesTheExpressionsAtTheFirstLookup() {
        // arrange
        CompiledRoutingExpressions expressions = new EmptyExpressions();
        expressions.register("com.acme.Event", "{event.id}", event -> "id");
        expressions.find("com.acme.Event", "{event.id}");

        // act & assert
        assertThatThrownBy(() -> expressions.register("com.acme.Event", "{event.late}", event -> "late"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Routing expressions are registered by the generated constructor only");

        // assert
        assertThat(expressions.find("com.acme.Event", "{event.late}")).isEmpty();
    }
}
