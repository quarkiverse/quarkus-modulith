package io.quarkiverse.modulith.core;

import java.util.List;

/**
 * A source factory that only contributes a root package.
 */
class RootOnlyFactory implements ApplicationModuleSourceFactory {

    @Override
    public List<String> getRootPackages() {
        return List.of("a");
    }
}
