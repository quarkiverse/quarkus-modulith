package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.List;

/**
 * A property read or a method call in an expression chain.
 *
 * @param name the property or method name
 * @param call whether it is a method call, written with parentheses
 * @param arguments the arguments of a method call
 */
record ExpressionMember(String name, boolean call, List<ExpressionChain> arguments) {

    ExpressionMember {
        arguments = List.copyOf(arguments);
    }

    static ExpressionMember property(String name) {
        return new ExpressionMember(name, false, List.of());
    }

    static ExpressionMember call(String name, List<ExpressionChain> arguments) {
        return new ExpressionMember(name, true, arguments);
    }
}
