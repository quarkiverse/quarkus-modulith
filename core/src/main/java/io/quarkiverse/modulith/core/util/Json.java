package io.quarkiverse.modulith.core.util;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders maps, collections, strings, enums and other values as JSON in Spring Modulith's layout.
 */
final class Json {

    private static final char FIRST_PRINTABLE = ' ';

    private Json() {
    }

    static String toString(Object object) {
        if (object instanceof String string) {
            return quote(string);
        }
        if (object instanceof Collection<?> collection) {
            return collection.isEmpty() ? "[]"
                    : collection.stream().map(Json::toString).collect(Collectors.joining(", ", "[ ", " ]"));
        }
        if (object instanceof Map<?, ?> map) {
            return map.isEmpty() ? "{}"
                    : map.entrySet().stream().map(it -> quote(it.getKey().toString()) + " : " + toString(it.getValue()))
                            .collect(Collectors.joining(", ", "{ ", " }"));
        }
        if (object instanceof Enum<?> value) {
            return quote(value.name());
        }
        return String.valueOf(object);
    }

    /**
     * @return the value as JSON string, with quotes, backslashes and control characters escaped
     */
    static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        value.chars().forEach(it -> result.append(escape((char) it)));
        return result.append('"').toString();
    }

    private static String escape(char character) {
        return switch (character) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> character < FIRST_PRINTABLE ? "\\u%04x".formatted((int) character) : String.valueOf(character);
        };
    }
}
