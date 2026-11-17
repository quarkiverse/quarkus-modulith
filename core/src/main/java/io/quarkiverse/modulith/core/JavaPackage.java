package io.quarkiverse.modulith.core;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * A Java package and the application types in it and in its sub-packages, like Spring Modulith's {@code JavaPackage}.
 */
public final class JavaPackage implements Iterable<JavaType>, Comparable<JavaPackage> {

    static final Set<String> PACKAGE_INFO_ANNOTATIONS = Set.of("io.quarkiverse.modulith.PackageInfo",
            "org.springframework.modulith.PackageInfo");

    private final PackageName name;
    private final List<JavaType> types;
    private final boolean includeSubPackages;
    /**
     * Computed on first use. Two threads may both compute it, which is harmless: the result is the same immutable value.
     */
    private volatile JavaPackages subPackages;

    private JavaPackage(PackageName name, Collection<JavaType> types, boolean includeSubPackages) {
        this.name = name;
        this.includeSubPackages = includeSubPackages;
        this.types = types.stream()
                .filter(it -> includeSubPackages ? name.contains(PackageName.ofType(it.getName()))
                        : name.equals(PackageName.ofType(it.getName())))
                .sorted()
                .toList();
    }

    /**
     * @param types the types to pick from, types outside the package are ignored
     * @param name the package name
     * @return the package with the given types of it and of its sub-packages
     */
    public static JavaPackage of(Collection<JavaType> types, String name) {
        return new JavaPackage(PackageName.of(name), types, true);
    }

    /**
     * @return whether the type stands for its package: a {@code package-info} type or a type annotated with
     *         {@code @PackageInfo}
     */
    public static boolean isPackageInfoType(JavaType type) {
        return type.isPackageInfo() || PACKAGE_INFO_ANNOTATIONS.stream().anyMatch(type::isMetaAnnotatedWith);
    }

    public String getName() {
        return name.getName();
    }

    public PackageName getPackageName() {
        return name;
    }

    /**
     * @return the last segment of the package name
     */
    public String getLocalName() {
        return name.getLocalName();
    }

    /**
     * @param pkg a sub-package of this package
     * @return the part of the sub-package's name after this package's name
     * @throws IllegalArgumentException if the given package is not a sub-package of this package
     */
    public String getTrailingName(JavaPackage pkg) {
        if (!pkg.getPackageName().isSubPackageOf(name)) {
            throw new IllegalArgumentException("Package %s is not a sub-package of %s".formatted(pkg.getName(), getName()));
        }
        return pkg.getName().substring(getName().length() + 1);
    }

    /**
     * @return the package without its sub-packages
     */
    public JavaPackage toSingle() {
        return new JavaPackage(name, types, false);
    }

    /**
     * @return the types of the package, and of its sub-packages unless this is a {@link #toSingle() single} package
     */
    public List<JavaType> getTypes() {
        return types;
    }

    /**
     * @return the public types other than {@code package-info}
     */
    public List<JavaType> getExposedTypes() {
        return that(it -> it.isPublic() && !it.isPackageInfo());
    }

    /**
     * @return the types matching the given predicate
     */
    public List<JavaType> that(Predicate<? super JavaType> predicate) {
        return types.stream().filter(predicate).toList();
    }

    /**
     * @return all sub-packages that contain types, transitively
     */
    public JavaPackages getSubPackages() {
        JavaPackages result = subPackages;
        if (Objects.isNull(result)) {
            result = includeSubPackages ? detectSubPackages() : JavaPackages.NONE;
            subPackages = result;
        }
        return result;
    }

    /**
     * @return the sub-packages directly below this package, including those that only contain sub-packages
     */
    public Collection<JavaPackage> getDirectSubPackages() {
        return getSubPackages().stream().filter(this::isDirectParentOf).toList();
    }

    /**
     * @return the sub-packages annotated with the given annotation on their {@code package-info} or
     *         {@code @PackageInfo} type
     */
    public Stream<JavaPackage> getSubPackagesAnnotatedWith(Class<? extends Annotation> annotation) {
        return getSubPackagesAnnotatedWith(Set.of(annotation.getName()));
    }

    Stream<JavaPackage> getSubPackagesAnnotatedWith(Set<String> annotations) {
        return getSubPackages().stream().filter(it -> it.isAnnotatedWith(annotations));
    }

    /**
     * @return the sub-packages matching the filter, which receives the package and its trailing name
     */
    public Stream<JavaPackage> getSubPackagesMatching(BiPredicate<JavaPackage, String> filter) {
        return getSubPackages().stream().filter(it -> filter.test(it, getTrailingName(it)));
    }

    /**
     * @return whether the {@code package-info} or {@code @PackageInfo} type of this package declares the annotation
     */
    public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
        return isAnnotatedWith(Set.of(annotation.getName()));
    }

    boolean isAnnotatedWith(Set<String> annotations) {
        return packageInfoTypes().anyMatch(type -> annotations.stream().anyMatch(type::isMetaAnnotatedWith));
    }

    /**
     * Loads the annotation of the package's {@code package-info} or {@code @PackageInfo} type with the context class
     * loader. In a native executable this only works for types registered for reflection, see
     * {@link JavaType#reflect()}.
     *
     * @return the annotation, if the package declares it
     */
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotation) {
        return packageInfoTypes().filter(it -> it.isAnnotatedWith(annotation))
                .map(it -> it.reflect().getAnnotation(annotation))
                .findFirst();
    }

    /**
     * @return the declaration of one of the given annotation types made on the package
     * @throws IllegalStateException if more than one type of the package declares it
     */
    Optional<AnnotationData> findDeclaration(Set<String> annotations) {
        List<JavaType> declaring = packageInfoTypes()
                .filter(it -> it.getDeclarations().stream().anyMatch(d -> annotations.contains(d.type())))
                .toList();
        if (declaring.size() > 1) {
            throw new IllegalStateException("Expected maximum of one type in package %s to be annotated with %s, but got %s!"
                    .formatted(name, String.join(" or ", new TreeSet<>(annotations)), declaring));
        }
        return declaring.stream().flatMap(it -> it.getDeclarations().stream())
                .filter(it -> annotations.contains(it.type()))
                .findFirst();
    }

    private Stream<JavaType> packageInfoTypes() {
        return types.stream()
                .filter(it -> name.equals(PackageName.ofType(it.getName())))
                .filter(JavaPackage::isPackageInfoType);
    }

    public boolean contains(JavaType type) {
        return types.contains(type);
    }

    public boolean contains(Class<?> type) {
        return contains(type.getName());
    }

    /**
     * @return whether the package contains the type with the given fully qualified name
     */
    public boolean contains(String typeName) {
        return types.stream().anyMatch(it -> it.getName().equals(typeName));
    }

    /**
     * @return whether a type with the given name would belong to this package, whether or not it exists
     */
    public boolean couldContain(Class<?> type) {
        return couldContain(type.getName());
    }

    public boolean couldContain(String typeName) {
        return name.contains(PackageName.ofType(typeName));
    }

    public Stream<JavaType> stream() {
        return types.stream();
    }

    @Override
    public Iterator<JavaType> iterator() {
        return types.iterator();
    }

    boolean isSubPackageOf(JavaPackage reference) {
        return name.isSubPackageOf(reference.name);
    }

    boolean isParentPackageOf(JavaPackage reference) {
        return reference.isSubPackageOf(this);
    }

    boolean isDirectParentOf(JavaPackage reference) {
        return name.isDirectParentOf(reference.name);
    }

    /**
     * @return this package without the types of the given packages that are its sub-packages
     */
    public JavaPackage without(JavaPackages exclusions) {
        List<JavaPackage> excluded = exclusions.stream().filter(this::isParentPackageOf).toList();
        if (excluded.isEmpty()) {
            return this;
        }
        return new JavaPackage(name, types.stream()
                .filter(type -> excluded.stream().noneMatch(it -> it.couldContain(type.getName())))
                .toList(), true);
    }

    private JavaPackages detectSubPackages() {
        Set<PackageName> names = new TreeSet<>();
        types.forEach(it -> name.expandUntil(PackageName.ofType(it.getName())).forEach(names::add));
        return new JavaPackages(names.stream().map(it -> new JavaPackage(it, types, true)).toList());
    }

    @Override
    public int compareTo(JavaPackage other) {
        return name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaPackage that && name.equals(that.name) && types.equals(that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, types);
    }

    @Override
    public String toString() {
        return name.getName();
    }
}
