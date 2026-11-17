package io.quarkiverse.modulith.events.deployment.externalization;

/**
 * A routing expression that cannot be parsed or compiled.
 */
final class RoutingExpressionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    RoutingExpressionException(String message) {
        super(message);
    }
}
