package io.quarkiverse.modulith.deployment.moduletest;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ApplicationModulesFactory;

/**
 * A factory a project registers for its module tests. It ignores the {@code @Modulith} type and analyzes the nested
 * fixture, so a test can tell its model from the one the build analyzes.
 */
public class NestedModulesFactory implements ApplicationModulesFactory {

    static final String NESTED = "io.quarkiverse.modulith.deployment.fixtures.nested";

    @Override
    public ApplicationModules of(Class<?> applicationClass) {
        return ApplicationModules.of(NESTED);
    }
}
