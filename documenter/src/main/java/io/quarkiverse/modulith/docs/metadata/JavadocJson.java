package io.quarkiverse.modulith.docs.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads the {@code javadoc.json} format: an array of types, each with a {@code name}, an optional {@code comment} and
 * optional {@code methods} with {@code name}, {@code signature} and optional {@code comment}. Only the JSON subset the
 * annotation processor writes is supported: objects, arrays, strings and {@code null}.
 */
final class JavadocJson {

    private final String text;
    private int position;

    private JavadocJson(String text) {
        this.text = text;
    }

    static List<TypeMetadata> read(String json) {
        JavadocJson parser = new JavadocJson(json);
        Object value = parser.value();
        parser.skipWhitespace();
        if (parser.position != json.length()) {
            throw parser.error("end of input");
        }
        List<TypeMetadata> result = new ArrayList<>();
        for (Object type : list(value, "the top level")) {
            Map<String, Object> members = object(type, "a type");
            List<MethodMetadata> methods = new ArrayList<>();
            for (Object method : list(members.getOrDefault("methods", List.of()), "methods")) {
                Map<String, Object> methodMembers = object(method, "a method");
                methods.add(new MethodMetadata(required(methodMembers, "name"), required(methodMembers, "signature"),
                        optional(methodMembers, "comment")));
            }
            result.add(new TypeMetadata(required(members, "name"), optional(members, "comment"), methods));
        }
        return result;
    }

    private static List<?> list(Object value, String what) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected an array for " + what + " in javadoc.json");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String what) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected an object for " + what + " in javadoc.json");
    }

    private static String required(Map<String, Object> members, String key) {
        return optional(members, key)
                .orElseThrow(() -> new IllegalArgumentException("Missing \"" + key + "\" in javadoc.json entry " + members));
    }

    private static Optional<String> optional(Map<String, Object> members, String key) {
        Object value = members.get(key);
        if (Objects.isNull(value) || value instanceof String) {
            return Optional.ofNullable((String) value);
        }
        throw new IllegalArgumentException("Expected a string for \"" + key + "\" in javadoc.json entry " + members);
    }

    private Object value() {
        skipWhitespace();
        char c = peek();
        if (c == '[') {
            return array();
        }
        if (c == '{') {
            return members();
        }
        if (c == '"') {
            return string();
        }
        if (text.startsWith("null", position)) {
            position += 4;
            return null;
        }
        throw error("a value");
    }

    private List<Object> array() {
        List<Object> result = new ArrayList<>();
        position++;
        skipWhitespace();
        if (peek() == ']') {
            position++;
            return result;
        }
        while (true) {
            result.add(value());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return result;
            }
            if (c != ',') {
                throw error("',' or ']'");
            }
        }
    }

    private Map<String, Object> members() {
        Map<String, Object> result = new LinkedHashMap<>();
        position++;
        skipWhitespace();
        if (peek() == '}') {
            position++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("a member name");
            }
            String key = string();
            skipWhitespace();
            if (next() != ':') {
                throw error("':'");
            }
            result.put(key, value());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return result;
            }
            if (c != ',') {
                throw error("',' or '}'");
            }
        }
    }

    private String string() {
        StringBuilder result = new StringBuilder();
        position++;
        while (true) {
            char c = next();
            if (c == '"') {
                return result.toString();
            }
            if (c == '\\') {
                result.append(escape());
            } else {
                result.append(c);
            }
        }
    }

    private char escape() {
        char c = next();
        return switch (c) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'u' -> unicode();
            case '"', '\\', '/' -> c;
            default -> throw error("an escape sequence");
        };
    }

    private char unicode() {
        if (position + 4 > text.length()) {
            throw error("four hex digits");
        }
        String hex = text.substring(position, position + 4);
        position += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw error("four hex digits");
        }
    }

    private void skipWhitespace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    private char peek() {
        if (position >= text.length()) {
            throw error("more input");
        }
        return text.charAt(position);
    }

    private char next() {
        char c = peek();
        position++;
        return c;
    }

    private IllegalArgumentException error(String expected) {
        return new IllegalArgumentException("Invalid javadoc.json: expected " + expected + " at offset " + position);
    }
}
