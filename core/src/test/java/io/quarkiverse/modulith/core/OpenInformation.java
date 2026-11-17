package io.quarkiverse.modulith.core;

import java.util.List;

/**
 * Module information that only declares no restriction.
 */
class OpenInformation implements ApplicationModuleInformation {

    @Override
    public List<String> getDeclaredDependencies() {
        return List.of(OPEN_TOKEN);
    }

    @Override
    public boolean isOpen() {
        return false;
    }
}
