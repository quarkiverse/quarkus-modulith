package io.quarkiverse.modulith.junit;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A test class run through the JUnit engine by {@link ModulithExecutionConditionTests}, with an invalid
 * configuration.
 */
class InvalidOnNoChangesSample {

    @RegisterExtension
    static final ModulithExecutionCondition CONDITION = new ModulithExecutionCondition(false,
            SampleConfigs::invalidOnNoChanges, Path.of("does-not-exist"), basePackage -> {
                throw new IllegalStateException("No module model expected");
            });

    @Test
    void test() {
    }
}
