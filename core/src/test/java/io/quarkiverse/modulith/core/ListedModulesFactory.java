package io.quarkiverse.modulith.core;

import java.util.List;

/**
 * A source factory that only lists module base packages.
 */
class ListedModulesFactory implements ApplicationModuleSourceFactory {

    @Override
    public List<String> getModuleBasePackages() {
        return List.of("a.b");
    }
}
