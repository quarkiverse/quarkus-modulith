package io.quarkiverse.modulith.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A sorted collection of {@link JavaPackage}s.
 */
public final class JavaPackages implements Iterable<JavaPackage> {

    /**
     * No packages.
     */
    public static final JavaPackages NONE = new JavaPackages(List.of());

    private final List<JavaPackage> packages;

    /**
     * @param packages the packages, sorted by name
     */
    public JavaPackages(Collection<JavaPackage> packages) {
        this.packages = packages.stream().sorted().toList();
    }

    public Stream<JavaPackage> stream() {
        return packages.stream();
    }

    public boolean contains(JavaPackage pkg) {
        return packages.contains(pkg);
    }

    /**
     * @return whether a type with the given name would belong to one of the packages
     */
    public boolean couldContain(Class<?> type) {
        return couldContain(type.getName());
    }

    public boolean couldContain(String typeName) {
        return packages.stream().anyMatch(it -> it.couldContain(typeName));
    }

    @Override
    public Iterator<JavaPackage> iterator() {
        return packages.iterator();
    }

    @Override
    public String toString() {
        return packages.toString();
    }
}
