package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.Map;

import io.quarkiverse.modulith.core.ModelData.ModuleData;

/**
 * Builds modules for the value type tests.
 */
final class Modules {

    private Modules() {
    }

    static ApplicationModule module(String name, String basePackage) {
        return new ApplicationModule(new ModuleData(name, name, basePackage, false, false, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of()), Map.of(), false);
    }
}
