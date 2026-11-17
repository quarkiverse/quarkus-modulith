package io.quarkiverse.modulith.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * A set of types a module exposes under a name, like Spring Modulith's {@code NamedInterface}. Every module has an
 * unnamed interface holding the public types of its base package.
 */
public final class NamedInterface implements Iterable<JavaType> {

    /**
     * The name of the interface formed by the module's base package.
     */
    public static final String UNNAMED_NAME = "<<UNNAMED>>";

    static final Set<String> ANNOTATIONS = Set.of("io.quarkiverse.modulith.NamedInterface",
            "org.springframework.modulith.NamedInterface");

    private final String name;
    private final List<JavaType> types;
    private final Set<String> typeNames;
    private final List<String> packages;

    private NamedInterface(String name, Collection<JavaType> types, List<String> packages) {
        this.name = name;
        this.types = types.stream().distinct().sorted().toList();
        this.typeNames = this.types.stream().map(JavaType::getName).collect(Collectors.toUnmodifiableSet());
        this.packages = packages;
    }

    /**
     * @return the interfaces declared by the {@code @NamedInterface} annotation of the given package, one per name,
     *         each with the public types of that package only
     * @throws IllegalArgumentException if the package is not annotated
     */
    public static List<NamedInterface> of(JavaPackage javaPackage) {
        JavaPackage single = javaPackage.toSingle();
        AnnotationData annotation = single.findDeclaration(ANNOTATIONS)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Couldn't find NamedInterface annotation on package %s!".formatted(javaPackage.getName())));
        List<JavaType> types = single.getExposedTypes();
        return names(annotation, javaPackage.getName()).stream()
                .map(it -> new NamedInterface(it, types, List.of(javaPackage.getName())))
                .toList();
    }

    /**
     * @return an interface with the given name and types
     */
    public static NamedInterface of(String name, Collection<JavaType> types) {
        return new NamedInterface(name, types, List.of());
    }

    static NamedInterface of(String name, Collection<JavaType> types, List<String> packages) {
        return new NamedInterface(name, types, packages);
    }

    /**
     * @return the unnamed interface of a closed module: the public types of the base package that neither carry
     *         {@code @NamedInterface} themselves nor sit in a package annotated with it
     * @throws IllegalStateException if a type of the base package uses {@code @NamedInterface} without a name
     */
    public static NamedInterface unnamed(JavaPackage javaPackage) {
        return unnamed(javaPackage, true);
    }

    /**
     * @return the unnamed interface of an open module: all public types of the module, transitively, except those that
     *         belong to an explicitly named interface
     */
    public static NamedInterface open(JavaPackage javaPackage) {
        return unnamed(javaPackage, false);
    }

    private static NamedInterface unnamed(JavaPackage javaPackage, boolean flatten) {
        List<JavaType> exposed = (flatten ? javaPackage.toSingle() : javaPackage).getExposedTypes();
        if (flatten) {
            List<JavaType> defaulted = exposed.stream()
                    .filter(type -> declaration(type).filter(it -> it.values("name").isEmpty()
                            && it.values("value").isEmpty()).isPresent())
                    .toList();
            if (!defaulted.isEmpty()) {
                throw new IllegalStateException("Cannot use named interface defaulting for type(s) %s located in base package!"
                        .formatted(FormattableType.format(defaulted)));
            }
        }
        return new NamedInterface(UNNAMED_NAME, exposed.stream()
                .filter(type -> declaration(type).isEmpty())
                .filter(type -> !residesInAnnotatedPackage(javaPackage, type))
                .toList(), List.of());
    }

    private static boolean residesInAnnotatedPackage(JavaPackage javaPackage, JavaType type) {
        return JavaPackage.of(javaPackage.getTypes(), type.getPackageName()).toSingle().isAnnotatedWith(ANNOTATIONS);
    }

    static Optional<AnnotationData> declaration(JavaType type) {
        return type.getDeclarations().stream().filter(it -> ANNOTATIONS.contains(it.type())).findFirst();
    }

    /**
     * @return the declared names, or the local name of the package when none is declared
     */
    static List<String> names(AnnotationData annotation, String packageName) {
        List<String> declared = Stream.of("value", "name")
                .flatMap(it -> annotation.values(it).stream().flatMap(List::stream))
                .distinct()
                .toList();
        return declared.isEmpty() ? List.of(PackageName.of(packageName).getLocalName()) : declared;
    }

    public String getName() {
        return name;
    }

    /**
     * @param qualifier the identifier of the module owning the interface
     * @return the name as used in {@code allowedDependencies}, for example {@code order :: events}
     */
    String getQualifiedName(ApplicationModuleIdentifier qualifier) {
        return qualifier + " :: " + name;
    }

    public boolean isUnnamed() {
        return name.equals(UNNAMED_NAME);
    }

    public boolean isNamed() {
        return !isUnnamed();
    }

    public boolean contains(JavaType type) {
        return typeNames.contains(type.getName());
    }

    public boolean contains(Class<?> type) {
        return contains(type.getName());
    }

    /**
     * @return whether the interface contains the type with the given fully qualified name
     */
    public boolean contains(String typeName) {
        return typeNames.contains(typeName);
    }

    /**
     * @return the names of the annotated packages this interface was declared on
     */
    public List<String> getPackages() {
        return packages;
    }

    public Stream<JavaType> asJavaTypes() {
        return types.stream();
    }

    NamedInterface merge(NamedInterface other) {
        List<JavaType> merged = new ArrayList<>(types);
        merged.addAll(other.types);
        List<String> mergedPackages = Stream.concat(packages.stream(), other.packages.stream()).distinct().toList();
        return new NamedInterface(name, merged, mergedPackages);
    }

    @Override
    public Iterator<JavaType> iterator() {
        return types.iterator();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NamedInterface that && name.equals(that.name) && types.equals(that.types);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + types.hashCode();
    }

    @Override
    public String toString() {
        return "NamedInterface: name=%s, types=[%s]".formatted(name,
                types.isEmpty() ? ""
                        : " " + types.stream().map(FormattableType::of)
                                .map(FormattableType::getAbbreviatedFullName).collect(Collectors.joining(", ")) + " ");
    }
}
