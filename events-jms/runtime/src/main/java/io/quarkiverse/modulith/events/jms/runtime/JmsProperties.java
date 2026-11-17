package io.quarkiverse.modulith.events.jms.runtime;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Turns the headers of an externalized event into JMS message properties. Jakarta Messaging only accepts property
 * names that are Java identifiers other than the message selector keywords, and values of the primitive wrapper types
 * or {@link String}. Other values are sent as their {@code toString()}, {@code null} values are left out.
 */
final class JmsProperties {

    private static final Set<String> KEYWORDS = Set.of("NULL", "TRUE", "FALSE", "NOT", "AND", "OR", "BETWEEN",
            "LIKE", "IN", "IS", "ESCAPE");
    private static final Set<Class<?>> TYPES = Set.of(Boolean.class, Byte.class, Short.class, Integer.class,
            Long.class, Float.class, Double.class, String.class);

    private JmsProperties() {
    }

    /**
     * @return the properties to set, in the order of the headers
     * @throws IllegalArgumentException if a header name is not a valid JMS property name
     */
    static Map<String, Object> of(Map<String, Object> headers) {
        Map<String, Object> properties = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (!isValidName(name)) {
                throw new IllegalArgumentException("The header " + name + " is not a valid JMS property name: use a"
                        + " Java identifier that is not a message selector keyword");
            }
            if (Objects.nonNull(value)) {
                properties.put(name, TYPES.contains(value.getClass()) ? value : value.toString());
            }
        });
        return properties;
    }

    static boolean isValidName(String name) {
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))
                || KEYWORDS.contains(name.toUpperCase(Locale.ROOT))) {
            return false;
        }
        return name.chars().skip(1).allMatch(Character::isJavaIdentifierPart);
    }
}
