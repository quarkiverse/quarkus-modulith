package io.quarkiverse.modulith.deployment.events;

/**
 * A literal: {@code number}, {@code string}, {@code boolean} or {@code null}.
 *
 * @param kind the kind of literal
 * @param text the literal text, without quotes for strings
 */
public record LiteralNode(LiteralKind kind, String text) implements ConditionNode {
}
