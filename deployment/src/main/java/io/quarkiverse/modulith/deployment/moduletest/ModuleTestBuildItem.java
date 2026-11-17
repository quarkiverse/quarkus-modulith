package io.quarkiverse.modulith.deployment.moduletest;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The setup of the current module test, computed once per build. Empty outside module tests.
 */
public final class ModuleTestBuildItem extends SimpleBuildItem {

    private final Optional<ModuleTestSetup> setup;

    public ModuleTestBuildItem(Optional<ModuleTestSetup> setup) {
        this.setup = setup;
    }

    public Optional<ModuleTestSetup> getSetup() {
        return setup;
    }
}
