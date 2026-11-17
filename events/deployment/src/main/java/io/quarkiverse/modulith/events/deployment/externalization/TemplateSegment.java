package io.quarkiverse.modulith.events.deployment.externalization;

/**
 * A part of a routing expression template: literal text or an expression.
 */
sealed interface TemplateSegment permits LiteralSegment, ChainSegment {
}
