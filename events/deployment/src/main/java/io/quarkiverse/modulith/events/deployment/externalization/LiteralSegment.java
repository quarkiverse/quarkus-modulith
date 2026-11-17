package io.quarkiverse.modulith.events.deployment.externalization;

/**
 * Literal text of a routing expression template.
 */
record LiteralSegment(String text) implements TemplateSegment {
}
