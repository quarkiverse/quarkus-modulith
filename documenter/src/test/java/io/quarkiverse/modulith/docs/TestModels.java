package io.quarkiverse.modulith.docs;

import java.util.List;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;

/**
 * Builds module models for tests without running a model builder.
 */
final class TestModels {

    private TestModels() {
    }

    static ModuleData module(String name, List<String> dependencies) {
        return new ModuleData(name, name.toUpperCase(), "com.acme." + name, false, false, List.of(), List.of(),
                dependencies, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    static ModuleData module(String name, List<String> dependencies, List<String> published, List<String> listened) {
        return new ModuleData(name, name.toUpperCase(), "com.acme." + name, false, false, List.of(), List.of(),
                dependencies, published, listened, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    static ModuleData restricted(String name, boolean open, List<String> allowed) {
        return new ModuleData(name, name.toUpperCase(), "com.acme." + name, open, true, allowed, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    static ApplicationModules modules(String systemName, ModuleData... modules) {
        return new ApplicationModules(new ModelData(systemName, "com.acme", List.of(modules), List.of(), ""));
    }
}
