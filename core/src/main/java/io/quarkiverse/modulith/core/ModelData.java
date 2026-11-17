package io.quarkiverse.modulith.core;

import java.util.List;

import io.quarkiverse.modulith.core.data.DependencyData;
import io.quarkiverse.modulith.core.data.EntryPointData;
import io.quarkiverse.modulith.core.data.EventTypeData;
import io.quarkiverse.modulith.core.data.TypeData;

/**
 * The module model computed by the analysis and recorded for runtime use. {@link ApplicationModules} is built from it,
 * both in a plain test ({@link ApplicationModules#of(Class)}) and in the running application.
 *
 * @param systemName the name of the system, empty if none is declared
 * @param basePackage the application's base package
 * @param modules the detected modules
 * @param violations the violations found by the analysis, meaningful when {@code verified} is set
 * @param mermaid the module diagram in Mermaid syntax
 * @param source the name of the type or package the model was built from
 * @param rootPackages the packages whose sub-packages are searched for modules, the base package first
 * @param sharedModules the identifiers of the modules every module may depend on
 * @param useFullyQualifiedModuleNames whether module identifiers default to full package names
 * @param types the types of the root packages
 * @param verified whether {@code violations} holds the result of the verification, otherwise the verification runs
 *        when the violations are first asked for
 *        <p>
 *        The nested records are published API that the deployment and runtime modules record. They stay nested until
 *        the next breaking release moves them to {@code io.quarkiverse.modulith.core.data}, next to {@link TypeData}.
 */
public record ModelData(String systemName, String basePackage, List<ModuleData> modules, List<ViolationData> violations,
        String mermaid, String source, List<String> rootPackages, List<String> sharedModules,
        boolean useFullyQualifiedModuleNames, List<TypeData> types, boolean verified) {

    /**
     * A verified model without type information, as the first versions of Quarkus Modulith recorded it.
     */
    public ModelData(String systemName, String basePackage, List<ModuleData> modules, List<ViolationData> violations,
            String mermaid) {
        this(systemName, basePackage, modules, violations, mermaid, basePackage,
                basePackage.isEmpty() ? List.of() : List.of(basePackage), List.of(), false, List.of(), true);
    }

    /**
     * @return a copy holding the given violations, marked as verified
     */
    public ModelData withViolations(List<ViolationData> violations, String mermaid) {
        return new ModelData(systemName, basePackage, modules, List.copyOf(violations), mermaid, source, rootPackages,
                sharedModules, useFullyQualifiedModuleNames, types, true);
    }

    /**
     * @return a copy with the given diagram, verified as before
     */
    public ModelData withMermaid(String mermaid) {
        return new ModelData(systemName, basePackage, modules, violations, mermaid, source, rootPackages, sharedModules,
                useFullyQualifiedModuleNames, types, verified);
    }

    /**
     * One application module.
     *
     * @param name the identifier
     * @param displayName the human readable name
     * @param basePackage the base package
     * @param open whether the module is open
     * @param restricted whether the module declares its allowed dependencies
     * @param allowedDependencies the declared allowed dependencies
     * @param namedInterfaces the named interfaces, the unnamed one included
     * @param dependencies the identifiers of the modules this module depends on directly
     * @param publishedEvents the names of the event types the module publishes
     * @param listenedEvents the names of the event types the module listens to
     * @param types the names of the module's types, without those of nested modules
     * @param excludedPackages the base packages of nested modules
     * @param qualifiedDependencies the references to types of other modules
     * @param beans the names of the bean types
     * @param events the event types the module publishes, with their creation sites
     * @param aggregateRoots the names of the aggregate root types
     * @param valueTypes the names of the value types
     * @param entryPoints the methods the framework calls into the module
     */
    public record ModuleData(String name, String displayName, String basePackage, boolean open, boolean restricted,
            List<String> allowedDependencies, List<NamedInterfaceData> namedInterfaces, List<String> dependencies,
            List<String> publishedEvents, List<String> listenedEvents, List<String> types, List<String> excludedPackages,
            List<DependencyData> qualifiedDependencies, List<String> beans, List<EventTypeData> events,
            List<String> aggregateRoots, List<String> valueTypes, List<EntryPointData> entryPoints) {

        /**
         * @param packageName the root package
         * @param types the names of the types of the root package itself
         * @param exposed the names of the public types among them
         * @return the artificial module holding the types of a root package, named {@code root:<package>}
         */
        public static ModuleData root(String packageName, List<String> types, List<String> exposed) {
            return new ModuleData("root:" + packageName, packageName, packageName, false, false, List.of(),
                    List.of(new NamedInterfaceData(NamedInterface.UNNAMED_NAME, List.of(), exposed)), List.of(),
                    List.of(), List.of(), types, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of());
        }
    }

    /**
     * A named interface of a module.
     *
     * @param name the name, {@code <<UNNAMED>>} for the base package interface
     * @param packages the annotated packages the interface was declared on
     * @param types the names of the types the interface consists of
     */
    public record NamedInterfaceData(String name, List<String> packages, List<String> types) {
    }

    /**
     * A violation of the module rules.
     *
     * @param kind the name of the {@link Violation.Kind}
     * @param module the identifier of the violating module
     * @param source where the violation was found
     * @param target what should not have been referenced, empty if not applicable
     * @param message the description
     */
    public record ViolationData(String kind, String module, String source, String target, String message) {
    }
}
