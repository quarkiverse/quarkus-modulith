package io.quarkiverse.modulith.runtime.model;

import java.util.List;
import java.util.function.Supplier;

import io.quarkiverse.modulith.core.ApplicationRuntime;
import io.quarkiverse.modulith.runtime.initializer.InitializerOrder;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Turns the data recorded at build time into the application runtime and initializer order beans.
 */
@Recorder
public class ModelRecorder {

    public Supplier<ApplicationRuntime> applicationRuntime(String id, String mainClass, List<String> rootPackages) {
        ApplicationRuntime runtime = new QuarkusApplicationRuntime(id, mainClass, rootPackages);
        return () -> runtime;
    }

    public Supplier<InitializerOrder> initializerOrder(List<String> classNames) {
        InitializerOrder order = new InitializerOrder(classNames);
        return () -> order;
    }
}
