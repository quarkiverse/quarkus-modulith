package io.quarkiverse.modulith.deployment.events;

/**
 * A short-circuit logical or.
 */
public record OrNode(ConditionNode left, ConditionNode right) implements ConditionNode {
}
