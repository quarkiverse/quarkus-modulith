package io.quarkiverse.modulith.test;

/**
 * The value a scenario waited for and the result of its stimulus.
 */
record ExecutionResult<S, T>(S awaited, T stimulusResult) {
}
