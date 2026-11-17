package io.quarkiverse.modulith.junit;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Computes the {@link ExecutionSettings} once per test run and keeps them in the root {@link Store} of JUnit.
 */
final class StateStore {

    static final String BASE_PACKAGE_PROPERTY = "quarkus.modulith.base-package";

    private final Store store;
    private final Supplier<Config> config;
    private final Path workingDirectory;

    /**
     * @param context the current extension context
     * @param config loads the configuration, called at most once per test run
     * @param workingDirectory the directory of the project under test
     */
    StateStore(ExtensionContext context, Supplier<Config> config, Path workingDirectory) {
        this.store = context.getRoot().getStore(Namespace.create(ModulithExecutionCondition.class));
        this.config = config;
        this.workingDirectory = workingDirectory;
    }

    /**
     * @return the settings of the current test run
     */
    ExecutionSettings getSettings() {
        return store.computeIfAbsent("settings", key -> {
            var configuration = config.get();
            return new ExecutionSettings(ChangesFactory.getChanges(configuration, workingDirectory),
                    configuration.getOptionalValue(BASE_PACKAGE_PROPERTY, String.class));
        }, ExecutionSettings.class);
    }
}
