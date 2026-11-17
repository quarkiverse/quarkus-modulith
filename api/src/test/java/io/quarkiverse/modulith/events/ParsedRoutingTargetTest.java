package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParsedRoutingTargetTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "orders|orders|null|orders::",
            "orders::o-1|orders|o-1|orders::o-1",
            "::o-1|null|o-1|::o-1",
            "'  ::o-1'|null|o-1|::o-1",
            "''|null|null|::",
            "orders::|orders|''|orders::"
    })
    void parsesDeclaredParts(String source, String target, String key, String text) {
        // act
        ParsedRoutingTarget parsed = ParsedRoutingTarget.parse(source);

        // assert
        assertThat(parsed.getTarget().orElse(null)).isEqualTo(target);
        assertThat(parsed.getKey().orElse(null)).isEqualTo(key);
        assertThat(parsed).hasToString(text);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "orders|fallback::fallback-key|orders::fallback-key",
            "orders::o-1|fallback::fallback-key|orders::o-1",
            "::o-1|fallback::fallback-key|fallback::o-1",
            "''|fallback::fallback-key|fallback::fallback-key",
            "''|fallback|fallback"
    })
    void fillsMissingPartsFromTheFallback(String source, String fallback, String expected) {
        // arrange
        ParsedRoutingTarget parsed = ParsedRoutingTarget.parse(source);

        // act
        RoutingTarget merged = parsed.withFallback(RoutingTarget.parse(fallback));

        // assert
        assertThat(merged).isEqualTo(RoutingTarget.parse(expected));
    }

    @Test
    void convertsADeclaredTarget() {
        // arrange
        ParsedRoutingTarget complete = ParsedRoutingTarget.parse("orders::o-1");

        // act
        RoutingTarget converted = complete.toRoutingTarget();

        // assert
        assertThat(converted).isEqualTo(RoutingTarget.forTarget("orders").andKey("o-1"));
    }

    @Test
    void cannotConvertAKeyWithoutTarget() {
        // arrange
        ParsedRoutingTarget keyOnly = ParsedRoutingTarget.parse("::o-1");

        // act & assert
        assertThatThrownBy(keyOnly::toRoutingTarget).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing target must not be blank");
    }

    @Test
    void requiresASource() {
        // act & assert
        assertThatThrownBy(() -> ParsedRoutingTarget.parse(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("source");
    }

    @Test
    void comparesDeclaredParts() {
        // arrange
        ParsedRoutingTarget parsed = ParsedRoutingTarget.parse("orders::o-1");

        // act
        ParsedRoutingTarget same = ParsedRoutingTarget.parse("orders::o-1");

        // assert
        assertThat(parsed).isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(ParsedRoutingTarget.parse("orders"))
                .isNotEqualTo(ParsedRoutingTarget.parse("other::o-1"))
                .isNotEqualTo(RoutingTarget.parse("orders::o-1"));
    }
}
