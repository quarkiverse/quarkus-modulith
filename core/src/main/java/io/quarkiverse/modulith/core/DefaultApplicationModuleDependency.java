package io.quarkiverse.modulith.core;

import java.util.Objects;

import io.quarkiverse.modulith.core.data.DependencyData;

/**
 * A recorded reference between two modules.
 */
final class DefaultApplicationModuleDependency implements ApplicationModuleDependency {

    private final DependencyData data;
    private final JavaType source;
    private final JavaType target;
    private final ApplicationModule targetModule;

    DefaultApplicationModuleDependency(DependencyData data, JavaType source, JavaType target,
            ApplicationModule targetModule) {
        this.data = data;
        this.source = source;
        this.target = target;
        this.targetModule = targetModule;
    }

    DependencyData getData() {
        return data;
    }

    @Override
    public String getSourceType() {
        return data.source();
    }

    @Override
    public String getTargetType() {
        return data.target();
    }

    @Override
    public DependencyType getDependencyType() {
        return DependencyType.valueOf(data.type());
    }

    @Override
    public ApplicationModule getTargetModule() {
        return targetModule;
    }

    @Override
    public JavaType getSourceJavaType() {
        return source;
    }

    @Override
    public JavaType getTargetJavaType() {
        return target;
    }

    @Override
    public String getDescription() {
        return data.description();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DefaultApplicationModuleDependency that && data.equals(that.data)
                && targetModule.getIdentifier().equals(that.targetModule.getIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, targetModule.getIdentifier());
    }

    @Override
    public String toString() {
        return getDependencyType().format(FormattableType.of(source), FormattableType.of(target));
    }
}
