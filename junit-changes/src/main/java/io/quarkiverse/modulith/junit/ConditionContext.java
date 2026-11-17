package io.quarkiverse.modulith.junit;

import java.util.Optional;

/**
 * The input of {@link TestExecutionCondition}.
 *
 * @param testClass the test class to decide about
 * @param changes the detected changes
 * @param basePackage the configured application base package, if any
 */
record ConditionContext(Class<?> testClass, Changes changes, Optional<String> basePackage) {
}
