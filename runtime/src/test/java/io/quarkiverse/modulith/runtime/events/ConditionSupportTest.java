package io.quarkiverse.modulith.runtime.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConditionSupportTest {

    @ParameterizedTest
    @MethodSource("equalities")
    void comparesForEqualityByValue(Object left, Object right, boolean expected) {
        // act
        boolean result = ConditionSupport.equal(left, right);

        // assert
        assertThat(result).isEqualTo(expected);
    }

    static List<Object[]> equalities() {
        return List.of(new Object[] { 100, new BigDecimal("100.0"), true },
                new Object[] { 100L, 101, false },
                new Object[] { 1.5d, new BigDecimal("1.5"), true },
                new Object[] { DayOfWeek.MONDAY, "MONDAY", true },
                new Object[] { DayOfWeek.MONDAY, "TUESDAY", false },
                new Object[] { "MONDAY", DayOfWeek.MONDAY, true },
                new Object[] { "FRIDAY", DayOfWeek.MONDAY, false },
                new Object[] { "a", "a", true },
                new Object[] { null, null, true },
                new Object[] { "a", null, false },
                new Object[] { 1, "1", false },
                new Object[] { DayOfWeek.MONDAY, 1, false },
                new Object[] { 1.5f, 1.5d, true },
                new Object[] { Double.NaN, Double.NaN, false },
                new Object[] { 1, Float.NaN, false },
                new Object[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, true },
                new Object[] { Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, true },
                new Object[] { Double.POSITIVE_INFINITY, Long.MAX_VALUE, false });
    }

    @ParameterizedTest
    @MethodSource("orderings")
    void ordersNumbersAndComparablesOfTheSameType(Object left, Object right, String operator, boolean expected) {
        // act
        boolean result = ConditionSupport.compare(left, right, operator);

        // assert
        assertThat(result).isEqualTo(expected);
    }

    static List<Object[]> orderings() {
        return List.of(new Object[] { 5, new BigDecimal("10"), "<", true },
                new Object[] { 10, 10L, "<", false },
                new Object[] { 10, 10L, "<=", true },
                new Object[] { 11, 10L, "<=", false },
                new Object[] { 11, 10L, ">", true },
                new Object[] { 10, 10L, ">", false },
                new Object[] { 10, 10L, ">=", true },
                new Object[] { 9, 10L, ">=", false },
                new Object[] { "b", "a", ">", true },
                new Object[] { DayOfWeek.MONDAY, DayOfWeek.FRIDAY, "<", true },
                new Object[] { Double.NaN, 1, "<", false },
                new Object[] { 1, Float.NaN, "<=", false },
                new Object[] { Double.NaN, 1, ">", false },
                new Object[] { Double.NaN, Double.NaN, ">=", false },
                new Object[] { Double.POSITIVE_INFINITY, Long.MAX_VALUE, ">", true },
                new Object[] { 1, Float.POSITIVE_INFINITY, "<", true },
                new Object[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, ">=", true });
    }

    @ParameterizedTest
    @MethodSource("unordered")
    void rejectsValuesThatCannotBeOrdered(Object left, Object right, String message) {
        // act & assert
        assertThatThrownBy(() -> ConditionSupport.compare(left, right, "<"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    static List<Object[]> unordered() {
        return List.of(new Object[] { "a", 1, "Cannot compare java.lang.String with java.lang.Integer" },
                new Object[] { "a", null, "Cannot compare java.lang.String with null" },
                new Object[] { new Object(), "a", "Cannot compare java.lang.Object with java.lang.String" },
                new Object[] { null, 1, "Cannot compare null with java.lang.Integer" },
                new Object[] { 1, "a", "Cannot compare java.lang.Integer with java.lang.String" });
    }

    @ParameterizedTest
    @MethodSource("operands")
    void rejectsAnUnknownOperator(Object left, Object right) {
        // act & assert
        assertThatThrownBy(() -> ConditionSupport.compare(left, right, "=="))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown comparison operator ==");
    }

    static List<Object[]> operands() {
        return List.of(new Object[] { 1, 2 }, new Object[] { Double.NaN, 2 });
    }

    @Test
    void readsNumberLiterals() {
        // arrange
        String literal = "12.50";

        // act
        Object number = ConditionSupport.number(literal);

        // assert
        assertThat(number).isEqualTo(new BigDecimal("12.50"));
    }

    @ParameterizedTest
    @MethodSource("truthiness")
    void isTruthyOnlyForTrue(Object value, boolean expected) {
        // act
        boolean truthy = ConditionSupport.truthy(value);

        // assert
        assertThat(truthy).isEqualTo(expected);
    }

    static List<Object[]> truthiness() {
        return List.of(new Object[] { Boolean.TRUE, true }, new Object[] { Boolean.FALSE, false },
                new Object[] { "true", false }, new Object[] { null, false });
    }
}
