package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RoutingTargetTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "orders|orders|",
            "' orders '|orders|",
            "orders::o-1|orders|o-1",
            "' orders :: o-1 '|orders|o-1",
            "orders::|orders|''",
            "orders::a::b|orders|a::b"
    })
    void parsesTargetAndKey(String source, String target, String key) {
        // act
        RoutingTarget parsed = RoutingTarget.parse(source);

        // assert
        assertThat(parsed.getTarget()).isEqualTo(target);
        assertThat(parsed.getKey().orElse(null)).isEqualTo(key);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "::key", " ::key" })
    void rejectsBlankTargets(String source) {
        // act & assert
        assertThatThrownBy(() -> RoutingTarget.parse(source)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing target must not be blank");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "  " })
    void rejectsBlankTargetsWhenBuilt(String target) {
        // act & assert
        assertThatThrownBy(() -> RoutingTarget.forTarget(target)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing target must not be blank");
    }

    @Test
    void rejectsNullSource() {
        // act & assert
        assertThatThrownBy(() -> RoutingTarget.parse(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("value");
    }

    @Test
    void copiesWithAndWithoutKey() {
        // arrange
        RoutingTarget plain = RoutingTarget.forTarget("orders");

        // act
        RoutingTarget keyed = plain.andKey(" o-1 ");
        RoutingTarget withKey = plain.withKey("o-2");
        RoutingTarget withNullKey = keyed.withKey(null);
        RoutingTarget withoutKey = keyed.withoutKey();

        // assert
        assertThat(keyed).hasToString("orders::o-1");
        assertThat(withKey).hasToString("orders::o-2");
        assertThat(withNullKey).isEqualTo(plain).hasToString("orders");
        assertThat(withoutKey).isEqualTo(plain);
        assertThat(plain.getKey()).isEmpty();
    }

    @Test
    void requiresAKeyToAdd() {
        // arrange
        RoutingTarget plain = RoutingTarget.forTarget("orders");

        // act & assert
        assertThatThrownBy(() -> plain.andKey(null)).isInstanceOf(NullPointerException.class).hasMessage("key");
    }

    @Test
    void comparesTargetAndKey() {
        // arrange
        RoutingTarget keyed = RoutingTarget.forTarget("orders").andKey("o-1");

        // act
        RoutingTarget parsed = RoutingTarget.parse("orders::o-1");

        // assert
        assertThat(keyed).isEqualTo(parsed)
                .hasSameHashCodeAs(parsed)
                .isNotEqualTo(RoutingTarget.forTarget("orders"))
                .isNotEqualTo(RoutingTarget.forTarget("other").andKey("o-1"))
                .isNotEqualTo("orders::o-1");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "orders|false|false",
            "orders::key|false|false",
            "orders::#{#this.id()}|true|true",
            "orders::{event.id}|true|true",
            "#{#this.region()}|false|true",
            "{event.region}::key|false|true",
            "orders-#{#this.id()}|false|false",
            "orders::key-{event.id}|false|false"
    })
    void detectsExpressions(String source, boolean keyExpression, boolean expression) {
        // arrange
        RoutingTarget target = RoutingTarget.parse(source);

        // act
        boolean key = target.hasKeyExpression();
        boolean any = target.hasExpression();

        // assert
        assertThat(key).isEqualTo(keyExpression);
        assertThat(any).isEqualTo(expression);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = { "#{x}|true", "{x}|true", "x{y}|false", "#x|false", "''|false", "|false" })
    void detectsExpressionValues(String value, boolean expected) {
        // act
        boolean expression = RoutingTarget.isExpression(value);

        // assert
        assertThat(expression).isEqualTo(expected);
    }
}
