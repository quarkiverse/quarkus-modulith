package io.quarkiverse.modulith.core;

import java.util.stream.Stream;

/**
 * The name of a Java package, compared segment by segment.
 */
public final class PackageName implements Comparable<PackageName> {

    /**
     * The name used for the default package.
     */
    public static final String DEFAULT = "<<default>>";

    private final String name;
    private final String[] segments;

    private PackageName(String name) {
        this.name = name;
        this.segments = name.split("\\.");
    }

    /**
     * @return the segments of the name, outermost first
     */
    Stream<String> segments() {
        return Stream.of(segments);
    }

    /**
     * @param name a package name, blank for the default package
     */
    public static PackageName of(String name) {
        return new PackageName(name.isBlank() ? DEFAULT : name);
    }

    /**
     * @return the package of the type with the given fully qualified name
     */
    public static PackageName ofType(String fullyQualifiedName) {
        return of(packageOf(fullyQualifiedName));
    }

    public static PackageName ofType(Class<?> type) {
        return of(type.getPackageName());
    }

    /**
     * @return the name of the package of the type with the given fully qualified name, empty for the default package
     */
    public static String packageOf(String typeName) {
        int dot = typeName.lastIndexOf('.');
        return dot < 0 ? "" : typeName.substring(0, dot);
    }

    static boolean isDefault(PackageName name) {
        return name.name.equals(DEFAULT);
    }

    public String getName() {
        return name;
    }

    /**
     * @return the last segment
     */
    public String getLocalName() {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * @return whether the given package is this one or one of its sub-packages
     */
    public boolean contains(PackageName reference) {
        return equals(reference) || reference.isSubPackageOf(this);
    }

    public boolean isSubPackageOf(PackageName reference) {
        int length = reference.name.length();
        return name.length() > length && name.charAt(length) == '.' && name.startsWith(reference.name);
    }

    boolean isDirectParentOf(PackageName reference) {
        return reference.hasParent() && equals(reference.getParent());
    }

    boolean hasParent() {
        return name.indexOf('.') >= 0;
    }

    PackageName getParent() {
        return of(name.substring(0, name.lastIndexOf('.')));
    }

    /**
     * Matches a package expression with {@code *} and {@code ?} wildcards, see {@link PackageExpression}.
     */
    boolean nameContainsOrMatches(String candidate) {
        return PackageExpression.of(candidate).matches(this);
    }

    /**
     * @return the packages between this one (exclusive) and the given sub-package (inclusive), outermost first
     */
    Stream<PackageName> expandUntil(PackageName reference) {
        if (!reference.isSubPackageOf(this)) {
            return Stream.empty();
        }
        if (isDirectParentOf(reference)) {
            return Stream.of(reference);
        }
        return Stream.concat(expandUntil(reference.getParent()), Stream.of(reference));
    }

    @Override
    public int compareTo(PackageName other) {
        String[] mine = segments;
        String[] theirs = other.segments;
        int common = Math.min(mine.length, theirs.length);
        for (int i = 0; i < common; i++) {
            int result = mine[i].compareTo(theirs[i]);
            if (result != 0) {
                return result;
            }
        }
        return mine.length - theirs.length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PackageName that && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
