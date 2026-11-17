package io.quarkiverse.modulith.core;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Defines named interfaces from the sub-packages of a module in code, for example in a custom
 * {@link ApplicationModuleDetectionStrategy}. Like Spring Modulith's {@code NamedInterfaces.Builder}.
 */
public final class NamedInterfacesBuilder {

    private final JavaPackage basePackage;
    private final boolean recursive;
    private final Predicate<JavaPackage> inclusions;
    private final Predicate<JavaPackage> exclusions;

    NamedInterfacesBuilder(JavaPackage basePackage) {
        this(basePackage, false, it -> false, it -> true);
    }

    private NamedInterfacesBuilder(JavaPackage basePackage, boolean recursive, Predicate<JavaPackage> inclusions,
            Predicate<JavaPackage> exclusions) {
        this.basePackage = basePackage;
        this.recursive = recursive;
        this.inclusions = inclusions;
        this.exclusions = exclusions;
    }

    /**
     * @return a builder that considers all sub-packages, not only the direct ones
     */
    public NamedInterfacesBuilder recursive() {
        return new NamedInterfacesBuilder(basePackage, true, inclusions, exclusions);
    }

    /**
     * @param names trailing package names or expressions with {@code *} and {@code ?}
     * @return a builder that includes the matching sub-packages
     */
    public NamedInterfacesBuilder matching(String... names) {
        return matching(List.of(names));
    }

    public NamedInterfacesBuilder matching(Collection<String> names) {
        return including(matchesTrailingName(names));
    }

    public NamedInterfacesBuilder including(Predicate<JavaPackage> inclusions) {
        return new NamedInterfacesBuilder(basePackage, recursive, inclusions, exclusions);
    }

    /**
     * @param expressions trailing package names or expressions with {@code *} and {@code ?}
     * @return a builder that leaves out the matching sub-packages
     */
    public NamedInterfacesBuilder excluding(String... expressions) {
        return excluding(List.of(expressions));
    }

    public NamedInterfacesBuilder excluding(Collection<String> expressions) {
        return excluding(Predicate.not(matchesTrailingName(expressions)));
    }

    /**
     * @param exclusions selects the packages to keep
     */
    public NamedInterfacesBuilder excluding(Predicate<JavaPackage> exclusions) {
        return new NamedInterfacesBuilder(basePackage, recursive, inclusions, exclusions);
    }

    /**
     * @return one interface per selected sub-package, named by its trailing name, plus the unnamed interface
     */
    public NamedInterfaces build() {
        Stream<JavaPackage> packages = recursive ? basePackage.getSubPackages().stream()
                : basePackage.getDirectSubPackages().stream();
        return NamedInterfaces.of(packages.filter(exclusions).filter(inclusions)
                .map(it -> NamedInterface.of(basePackage.getTrailingName(it), it.getTypes(), List.of(it.getName())))
                .toList())
                .and(List.of(NamedInterface.unnamed(basePackage)));
    }

    private Predicate<JavaPackage> matchesTrailingName(Collection<String> names) {
        List<PackageExpression> expressions = names.stream().map(PackageExpression::of).toList();
        return it -> {
            PackageName trailingName = PackageName.of(basePackage.getTrailingName(it));
            return expressions.stream().anyMatch(expression -> expression.matches(trailingName));
        };
    }
}
