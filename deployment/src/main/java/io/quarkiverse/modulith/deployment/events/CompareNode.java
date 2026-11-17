package io.quarkiverse.modulith.deployment.events;

/**
 * A comparison.
 *
 * @param operator one of {@code ==}, {@code !=}, {@code <}, {@code <=}, {@code >}, {@code >=}
 */
public record CompareNode(String operator, ConditionNode left, ConditionNode right) implements ConditionNode {
}
