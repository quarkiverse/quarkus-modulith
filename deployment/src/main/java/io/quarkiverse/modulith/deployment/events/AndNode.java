package io.quarkiverse.modulith.deployment.events;

/**
 * A short-circuit logical and.
 */
public record AndNode(ConditionNode left, ConditionNode right) implements ConditionNode {
}
