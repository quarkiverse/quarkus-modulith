package io.quarkiverse.modulith.runtime.initializer;

import java.util.List;
import java.util.Objects;

import io.quarkiverse.modulith.ApplicationModuleInitializer;

/**
 * Records its call, then throws the failure it was given, if any.
 */
record OrdersInitializer(List<String> calls, RuntimeException failure) implements ApplicationModuleInitializer {

    @Override
    public void initialize() {
        calls.add("initialize orders");
        if (Objects.nonNull(failure)) {
            throw failure;
        }
    }
}
