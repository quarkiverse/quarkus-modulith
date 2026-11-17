package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A type as written in a generic signature.
 *
 * @param name the binary class name, or the name of a type variable
 * @param variable whether this is a type variable
 * @param wildcard {@code ' '} for a plain type argument, {@code '+'} for {@code ? extends}, {@code '-'} for
 *        {@code ? super}, {@code '*'} for an unbounded {@code ?}
 * @param dimensions the array dimensions
 * @param arguments the type arguments
 */
record GenericType(String name, boolean variable, char wildcard, int dimensions, List<GenericType> arguments) {

    /**
     * @return the classes inside the type arguments, at any depth, type variables left out
     */
    Stream<String> argumentClasses() {
        return arguments.stream().flatMap(GenericType::involvedClasses);
    }

    /**
     * @return this class, unless it is a type variable or an unbounded wildcard, and the classes of its arguments
     */
    Stream<String> involvedClasses() {
        Stream<String> self = variable || wildcard == '*' ? Stream.empty() : Stream.of(name);
        return Stream.concat(self, argumentClasses());
    }

    /**
     * @return the type as source code would show it, {@code java.util.List<com.acme.Order>}
     */
    String render() {
        String base = switch (wildcard) {
            case '*' -> "?";
            case '+' -> "? extends " + plain();
            case '-' -> "? super " + plain();
            default -> plain();
        };
        return base;
    }

    private String plain() {
        String args = arguments.isEmpty() ? ""
                : arguments.stream().map(GenericType::render).collect(Collectors.joining(", ", "<", ">"));
        return name + args + "[]".repeat(dimensions);
    }
}
