package io.quarkiverse.modulith.deployment.events;

/**
 * A logical negation.
 */
public record NotNode(ConditionNode operand) implements ConditionNode {
}
