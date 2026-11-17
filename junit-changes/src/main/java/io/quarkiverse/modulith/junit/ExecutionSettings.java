package io.quarkiverse.modulith.junit;

import java.util.Optional;

/**
 * What change-aware test execution computes once per test run.
 *
 * @param changes the detected changes
 * @param basePackage the configured {@code quarkus.modulith.base-package}, if any
 */
record ExecutionSettings(Changes changes, Optional<String> basePackage) {
}
