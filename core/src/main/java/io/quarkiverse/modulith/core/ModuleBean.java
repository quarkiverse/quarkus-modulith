package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.Objects;

/**
 * A bean type of a module, like Spring Modulith's {@code SpringBean}.
 */
public final class ModuleBean {

    private final JavaType type;
    private final ApplicationModule module;

    ModuleBean(JavaType type, ApplicationModule module) {
        this.type = type;
        this.module = module;
    }

    public JavaType getType() {
        return type;
    }

    public String getFullyQualifiedTypeName() {
        return type.getName();
    }

    /**
     * @return whether other modules may use the bean: its type, or one of its interfaces in the module, is exposed
     */
    public boolean isApiBean() {
        return module.isExposed(type) || getInterfacesWithinModule().stream().anyMatch(module::isExposed);
    }

    /**
     * @return the interfaces the bean type implements directly that belong to the module
     */
    public List<JavaType> getInterfacesWithinModule() {
        return type.getInterfaceNames().stream().flatMap(it -> module.getType(it).stream()).toList();
    }

    public ArchitecturallyEvidentType toArchitecturallyEvidentType() {
        return ArchitecturallyEvidentType.of(type);
    }

    public boolean isAssignableTo(Class<?> other) {
        return type.isAssignableTo(other);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ModuleBean that && type.equals(that.type)
                && module.getIdentifier().equals(that.module.getIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, module.getIdentifier());
    }

    @Override
    public String toString() {
        return type.getName();
    }
}
