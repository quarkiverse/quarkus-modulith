package io.quarkiverse.modulith.core;

import java.util.Objects;
import java.util.Optional;

/**
 * One entry of a module's allowed dependencies: a module, one of its named interfaces ({@code module::name}), all of
 * its named interfaces ({@code module::*}), or everything it exposes ({@code module::**}, a Quarkus Modulith
 * addition). Like Spring Modulith's {@code ApplicationModule.AllowedDependency}.
 */
public final class AllowedDependency {

    static final String INVALID_EXPLICIT_MODULE_DEPENDENCY = "Invalid explicit module dependency in %s! No module found with name '%s'.";
    static final String INVALID_NAMED_INTERFACE_DECLARATION = "No named interface named '%s' found! Original dependency declaration: %s -> %s.";
    static final String INVALID_DECLARATION_FORMAT = "Invalid allowed dependency '%s' in %s! Use module, module::name,"
            + " module::* or module::**.";

    private static final String WILDCARD = "*";
    private static final String EVERYTHING = "**";

    private final ApplicationModule target;
    private final Optional<NamedInterface> namedInterface;
    private final boolean everything;

    private AllowedDependency(ApplicationModule target, Optional<NamedInterface> namedInterface, boolean everything) {
        this.target = target;
        this.namedInterface = namedInterface;
        this.everything = everything;
    }

    /**
     * @param identifier {@code module}, {@code module::name}, {@code module::*} or {@code module::**}
     * @param source the module declaring the dependency
     * @param modules the modules to look the target up in
     * @throws IllegalArgumentException if the module or the named interface does not exist, or if the identifier has
     *         more than one {@code ::}
     */
    static AllowedDependency of(String identifier, ApplicationModule source, ApplicationModules modules) {
        String[] segments = identifier.split("::");
        if (segments.length > 2) {
            throw new IllegalArgumentException(INVALID_DECLARATION_FORMAT.formatted(identifier, source.getIdentifier()));
        }
        String targetModuleName = segments[0].trim();
        Optional<String> namedInterfaceName = segments.length > 1 ? Optional.of(segments[1].trim()) : Optional.empty();
        ApplicationModule target = modules.getModuleByName(targetModuleName).orElseThrow(() -> new IllegalArgumentException(
                INVALID_EXPLICIT_MODULE_DEPENDENCY.formatted(source.getIdentifier(), targetModuleName)));
        if (namedInterfaceName.filter(it -> it.equals(WILDCARD) || it.equals(EVERYTHING)).isPresent()) {
            return new AllowedDependency(target, Optional.empty(), namedInterfaceName.get().equals(EVERYTHING));
        }
        NamedInterfaces namedInterfaces = target.getNamedInterfaceDetails();
        NamedInterface namedInterface = namedInterfaceName.map(name -> namedInterfaces.getByName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        INVALID_NAMED_INTERFACE_DECLARATION.formatted(name, source.getIdentifier(), identifier))))
                .orElseGet(namedInterfaces::getUnnamedInterface);
        return new AllowedDependency(target, Optional.of(namedInterface), false);
    }

    /**
     * @return a dependency to the unnamed interface of the given module
     */
    static AllowedDependency to(ApplicationModule module) {
        return new AllowedDependency(module, Optional.of(module.getNamedInterfaceDetails().getUnnamedInterface()), false);
    }

    public ApplicationModule getTargetModule() {
        return target;
    }

    /**
     * @return the allowed named interface, empty for {@code module::*} and {@code module::**}
     */
    public Optional<NamedInterface> getTargetNamedInterface() {
        return namedInterface;
    }

    boolean contains(JavaType type) {
        NamedInterfaces interfaces = target.getNamedInterfaceDetails();
        return namedInterface.map(it -> it.contains(type)).orElseGet(() -> everything
                ? interfaces.getNamedInterfacesContaining(type).findAny().isPresent()
                : interfaces.containsInExplicitInterface(type));
    }

    @Override
    public String toString() {
        String result = target.getIdentifier().toString();
        return namedInterface.map(it -> it.isUnnamed() ? result : result + " :: " + it.getName())
                .orElseGet(() -> result + " :: " + (everything ? EVERYTHING : WILDCARD));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AllowedDependency that && target.equals(that.target)
                && namedInterface.equals(that.namedInterface) && everything == that.everything;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, namedInterface, everything);
    }
}
