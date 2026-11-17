package io.quarkiverse.modulith.deployment.moduletest;

import java.util.List;
import java.util.Optional;

public record FixedModuleTestConfig(Optional<String> module, BootstrapMode bootstrapMode,
        Optional<List<String>> extraModules, boolean verifyAutomatically)
        implements
            ModuleTestConfig {

    @Override
    public boolean skipOptimizations() {
        return false;
    }

    @Override
    public Optional<String> onNoChanges() {
        return Optional.empty();
    }

    @Override
    public Optional<String> fileModificationDetector() {
        return Optional.empty();
    }

    @Override
    public Optional<String> referenceCommit() {
        return Optional.empty();
    }
}
