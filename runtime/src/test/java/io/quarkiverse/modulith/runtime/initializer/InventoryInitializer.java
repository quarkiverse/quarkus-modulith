package io.quarkiverse.modulith.runtime.initializer;

import java.util.List;

import io.quarkiverse.modulith.ApplicationModuleInitializer;

record InventoryInitializer(List<String> calls) implements ApplicationModuleInitializer {

    @Override
    public void initialize() {
        calls.add("initialize inventory");
    }
}
