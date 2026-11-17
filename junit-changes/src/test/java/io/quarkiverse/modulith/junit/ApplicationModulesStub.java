package io.quarkiverse.modulith.junit;

import io.quarkiverse.modulith.core.ApplicationModules;

/**
 * A module model factory for tests that must not build a model.
 */
final class ApplicationModulesStub {

    private ApplicationModulesStub() {
    }

    static ApplicationModules unavailable(String basePackage) {
        throw new UnsupportedOperationException("No model for " + basePackage);
    }
}
