package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What the application declares about its module structure as a whole, through {@code @Modulith},
 * {@code @Modulithic} (or their Spring Modulith counterparts) and {@code quarkus.modulith.*} properties. Like Spring
 * Modulith's {@code ModulithMetadata}.
 */
public final class ModulithMetadata {

    private final Object source;
    private final Optional<String> systemName;
    private final List<String> sharedModules;
    private final List<String> basePackages;
    private final boolean useFullyQualifiedModuleNames;

    private ModulithMetadata(Object source, Optional<String> systemName, List<String> sharedModules,
            List<String> basePackages, boolean useFullyQualifiedModuleNames) {
        this.source = source;
        this.systemName = systemName;
        this.sharedModules = sharedModules;
        this.basePackages = basePackages;
        this.useFullyQualifiedModuleNames = useFullyQualifiedModuleNames;
    }

    /**
     * Reads the metadata of an application from the given type, which may but need not carry {@code @Modulith}: the
     * base package is the type's package either way.
     */
    public static ModulithMetadata of(Class<?> type) {
        return ApplicationModules.of(type).getMetadata();
    }

    static ModulithMetadata of(ModelData data) {
        return new ModulithMetadata(data.source(), Optional.of(data.systemName()).filter(it -> !it.isEmpty()),
                data.sharedModules(), data.rootPackages(), data.useFullyQualifiedModuleNames());
    }

    /**
     * @return the name of the type or package the model was built from
     */
    public Object getSource() {
        return source;
    }

    public boolean useFullyQualifiedModuleNames() {
        return useFullyQualifiedModuleNames;
    }

    public Stream<ApplicationModuleIdentifier> getSharedModuleIdentifiers() {
        return sharedModules.stream().map(ApplicationModuleIdentifier::of);
    }

    public Optional<String> getSystemName() {
        return systemName;
    }

    /**
     * @return the root packages: the base package, then the additional ones
     */
    public List<String> getBasePackages() {
        return basePackages;
    }

    @Override
    public String toString() {
        return "ModulithMetadata[source=" + source + ", basePackages=" + basePackages + "]";
    }
}
