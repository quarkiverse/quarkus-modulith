package io.quarkiverse.modulith.deployment.events;

/**
 * A node of a parsed listener condition.
 */
public sealed interface ConditionNode permits LiteralNode, PathNode, CompareNode, AndNode, OrNode, NotNode {
}
