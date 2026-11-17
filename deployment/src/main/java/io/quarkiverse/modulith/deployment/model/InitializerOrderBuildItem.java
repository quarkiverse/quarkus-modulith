package io.quarkiverse.modulith.deployment.model;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The {@code ApplicationModuleInitializer} bean classes in the order they run.
 */
public final class InitializerOrderBuildItem extends SimpleBuildItem {

    private final List<String> classNames;

    public InitializerOrderBuildItem(List<String> classNames) {
        this.classNames = List.copyOf(classNames);
    }

    /**
     * @return the class names, initializers of modules other modules depend on first
     */
    public List<String> getClassNames() {
        return classNames;
    }
}
