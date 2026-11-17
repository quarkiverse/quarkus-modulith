package io.quarkiverse.modulith.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * The named interfaces of a module, the unnamed one included, sorted by name. Like Spring Modulith's
 * {@code NamedInterfaces}.
 */
public final class NamedInterfaces implements Iterable<NamedInterface> {

    /**
     * No interfaces.
     */
    public static final NamedInterfaces NONE = new NamedInterfaces(List.of());

    private final List<NamedInterface> namedInterfaces;

    private NamedInterfaces(List<NamedInterface> namedInterfaces) {
        this.namedInterfaces = namedInterfaces.stream().sorted(Comparator.comparing(NamedInterface::getName)).toList();
    }

    /**
     * @return the interfaces of a module with the given base package: those of an open module when the information
     *         says so, otherwise the discovered ones
     */
    public static NamedInterfaces of(JavaPackage basePackage, ApplicationModuleInformation information) {
        return information.isOpen() ? forOpen(basePackage) : discoverNamedInterfaces(basePackage);
    }

    /**
     * @return the unnamed interface, the interfaces of annotated packages and those of annotated types
     */
    public static NamedInterfaces discoverNamedInterfaces(JavaPackage basePackage) {
        return of(List.of(NamedInterface.unnamed(basePackage)))
                .and(ofAnnotatedPackages(basePackage))
                .and(ofAnnotatedTypes(basePackage.getTypes()));
    }

    /**
     * @return the interfaces of an open module: all its public types in the unnamed interface, plus the explicitly
     *         named ones
     */
    public static NamedInterfaces forOpen(JavaPackage basePackage) {
        return of(List.of(NamedInterface.open(basePackage)))
                .and(ofAnnotatedPackages(basePackage))
                .and(ofAnnotatedTypes(basePackage.getTypes()));
    }

    /**
     * @return a builder defining named interfaces from the sub-packages of the given package
     */
    public static NamedInterfacesBuilder builder(JavaPackage basePackage) {
        return new NamedInterfacesBuilder(basePackage);
    }

    /**
     * @return the given interfaces
     */
    public static NamedInterfaces of(List<NamedInterface> interfaces) {
        return interfaces.isEmpty() ? NONE : new NamedInterfaces(interfaces);
    }

    static NamedInterfaces ofAnnotatedPackages(JavaPackage basePackage) {
        return of(basePackage.getSubPackagesAnnotatedWith(NamedInterface.ANNOTATIONS)
                .flatMap(it -> NamedInterface.of(it).stream())
                .toList());
    }

    static List<NamedInterface> ofAnnotatedTypes(List<JavaType> types) {
        Map<String, Set<JavaType>> mappings = new LinkedHashMap<>();
        types.stream().filter(it -> !JavaPackage.isPackageInfoType(it)).forEach(type -> NamedInterface.declaration(type)
                .ifPresent(annotation -> NamedInterface.names(annotation, type.getPackageName()).forEach(
                        name -> mappings.computeIfAbsent(name, it -> new HashSet<>())
                                .addAll(propagates(annotation) ? relatedTypesOf(type, types) : List.of(type)))));
        return mappings.entrySet().stream().map(it -> NamedInterface.of(it.getKey(), it.getValue())).toList();
    }

    private static boolean propagates(AnnotationData annotation) {
        return !annotation.value("propagate").filter("false"::equals).isPresent();
    }

    /**
     * @return the type and the public types of the given ones that appear in its public signatures, transitively
     */
    static Set<JavaType> relatedTypesOf(JavaType type, List<JavaType> candidates) {
        Set<JavaType> result = new HashSet<>();
        if (type.isPublic()) {
            Map<String, JavaType> publicCandidates = new HashMap<>();
            candidates.stream().filter(JavaType::isPublic).forEach(it -> publicCandidates.putIfAbsent(it.getName(), it));
            collect(type, publicCandidates, result);
        }
        return result;
    }

    private static void collect(JavaType type, Map<String, JavaType> candidates, Set<JavaType> result) {
        if (!result.add(type)) {
            return;
        }
        type.getData().signatureTypes().stream()
                .flatMap(it -> Optional.ofNullable(candidates.get(it)).stream())
                .forEach(it -> collect(it, candidates, result));
    }

    /**
     * @return whether there is more than the unnamed interface
     */
    public boolean hasExplicitInterfaces() {
        return namedInterfaces.stream().anyMatch(NamedInterface::isNamed);
    }

    public Stream<NamedInterface> stream() {
        return namedInterfaces.stream();
    }

    public Optional<NamedInterface> getByName(String name) {
        return namedInterfaces.stream().filter(it -> it.getName().equals(name)).findFirst();
    }

    /**
     * @throws IllegalStateException if there is no unnamed interface
     */
    public NamedInterface getUnnamedInterface() {
        return namedInterfaces.stream()
                .filter(NamedInterface::isUnnamed)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No unnamed interface found!"));
    }

    public Stream<NamedInterface> getNamedInterfacesContaining(JavaType type) {
        return namedInterfaces.stream().filter(it -> it.contains(type));
    }

    /**
     * @return the interfaces containing the type with the given fully qualified name
     */
    public Stream<NamedInterface> getNamedInterfacesContaining(String typeName) {
        return namedInterfaces.stream().filter(it -> it.contains(typeName));
    }

    /**
     * @return whether a named (not the unnamed) interface contains the type
     */
    public boolean containsInExplicitInterface(JavaType type) {
        return containsInExplicitInterface(type.getName());
    }

    public boolean containsInExplicitInterface(String typeName) {
        return getNamedInterfacesContaining(typeName).anyMatch(NamedInterface::isNamed);
    }

    /**
     * @return these interfaces and the given ones, all interfaces with the same name merged into one, also when several
     *         of the given ones share a name, for example two packages {@code a.spi} and {@code b.spi} both annotated
     *         with {@code @NamedInterface}
     */
    public NamedInterfaces and(Iterable<NamedInterface> others) {
        Map<String, NamedInterface> merged = new LinkedHashMap<>();
        namedInterfaces.forEach(it -> merged.merge(it.getName(), it, NamedInterface::merge));
        others.forEach(it -> merged.merge(it.getName(), it, NamedInterface::merge));
        return of(List.copyOf(merged.values()));
    }

    @Override
    public Iterator<NamedInterface> iterator() {
        return namedInterfaces.iterator();
    }

    @Override
    public String toString() {
        return namedInterfaces.stream().map(NamedInterface::toString).collect(Collectors.joining(System.lineSeparator()));
    }
}
