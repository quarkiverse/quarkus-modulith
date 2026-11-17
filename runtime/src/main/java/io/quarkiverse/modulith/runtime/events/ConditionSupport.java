package io.quarkiverse.modulith.runtime.events;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Operations the listener conditions compiled at build time call at runtime. Values arrive boxed. Numbers compare by
 * value whatever their type, an enum compares with a string by its name, other values compare with
 * {@link Comparable#compareTo} or {@link Object#equals}. A {@code NaN} equals nothing and is neither smaller nor
 * greater than any number, like with Java's operators, and infinities compare as {@code double}s.
 */
public final class ConditionSupport {

    private ConditionSupport() {
    }

    /**
     * @param text a number literal of the condition
     * @return the literal as a {@link BigDecimal}
     */
    public static Object number(String text) {
        return new BigDecimal(text);
    }

    /**
     * @return {@code true} only for {@link Boolean#TRUE}
     */
    public static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value);
    }

    public static boolean equal(Object left, Object right) {
        if (left instanceof Number l && right instanceof Number r) {
            return !unordered(l, r) && order(l, r) == 0;
        }
        if (left instanceof Enum<?> e && right instanceof String s) {
            return e.name().equals(s);
        }
        if (left instanceof String s && right instanceof Enum<?> e) {
            return e.name().equals(s);
        }
        return Objects.equals(left, right);
    }

    /**
     * @param operator one of {@code <}, {@code <=}, {@code >}, {@code >=}
     * @throws IllegalArgumentException if the values cannot be ordered or the operator is unknown
     */
    public static boolean compare(Object left, Object right, String operator) {
        boolean unordered = left instanceof Number l && right instanceof Number r && unordered(l, r);
        int result = unordered ? 0 : order(left, right);
        return switch (operator) {
            case "<" -> !unordered && result < 0;
            case "<=" -> !unordered && result <= 0;
            case ">" -> !unordered && result > 0;
            case ">=" -> !unordered && result >= 0;
            default -> throw new IllegalArgumentException("Unknown comparison operator " + operator);
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static int order(Object left, Object right) {
        if (left instanceof Number l && right instanceof Number r) {
            return finite(l) && finite(r) ? decimal(l).compareTo(decimal(r))
                    : Double.compare(l.doubleValue(), r.doubleValue());
        }
        if (left instanceof Comparable l && Objects.nonNull(right) && left.getClass() == right.getClass()) {
            return l.compareTo(right);
        }
        throw new IllegalArgumentException("Cannot compare " + typeName(left) + " with " + typeName(right));
    }

    private static boolean unordered(Number left, Number right) {
        return isNaN(left) || isNaN(right);
    }

    private static boolean isNaN(Number number) {
        return floatingPoint(number) && Double.isNaN(number.doubleValue());
    }

    /**
     * @return whether the number has a decimal representation, which a {@code NaN} or an infinity has not
     */
    private static boolean finite(Number number) {
        return !floatingPoint(number) || Double.isFinite(number.doubleValue());
    }

    private static boolean floatingPoint(Number number) {
        return number instanceof Double || number instanceof Float;
    }

    private static BigDecimal decimal(Number number) {
        return number instanceof BigDecimal d ? d : new BigDecimal(number.toString());
    }

    private static String typeName(Object value) {
        return Objects.isNull(value) ? "null" : value.getClass().getName();
    }
}
