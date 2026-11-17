package io.quarkiverse.modulith.events.deployment.externalization;

/**
 * A routing expression compiled for an event type.
 *
 * @param type the event type
 * @param expression the expression as declared
 * @param className the generated class
 */
record CompiledExpression(String type, String expression, String className) {
}
