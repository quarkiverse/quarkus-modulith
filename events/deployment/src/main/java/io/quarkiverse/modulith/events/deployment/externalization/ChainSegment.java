package io.quarkiverse.modulith.events.deployment.externalization;

/**
 * An expression of a routing expression template.
 */
record ChainSegment(ExpressionChain chain) implements TemplateSegment {
}
