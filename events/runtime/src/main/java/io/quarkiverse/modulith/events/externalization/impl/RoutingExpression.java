package io.quarkiverse.modulith.events.externalization.impl;

/**
 * A routing expression compiled at build time. Implemented by generated classes, not by applications.
 */
@FunctionalInterface
public interface RoutingExpression {

    /**
     * @return the value of the expression for the event, {@code null} when it evaluates to {@code null}
     */
    String evaluate(Object event);
}
