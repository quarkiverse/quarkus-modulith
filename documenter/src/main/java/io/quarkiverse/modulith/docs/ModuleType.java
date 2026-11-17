package io.quarkiverse.modulith.docs;

import java.util.Set;

/**
 * A type of an application module, with what the documentation needs to know about it.
 *
 * @param name the binary name of the type, for example {@code com.acme.order.Order$Line}
 * @param publicType whether the type is public
 * @param exposed whether other modules may use the type: it is part of the module's API
 * @param annotations the names of the annotations declared on the type
 * @param metaAnnotations the names of the annotations found on the type's annotations, transitively
 * @param packageAnnotations the names of the annotations, and their meta-annotations, declared on the type's package
 * @param supertypes the names of every superclass and interface of the type, transitively
 */
public record ModuleType(String name, boolean publicType, boolean exposed, Set<String> annotations,
        Set<String> metaAnnotations, Set<String> packageAnnotations, Set<String> supertypes) {

    public ModuleType {
        annotations = Set.copyOf(annotations);
        metaAnnotations = Set.copyOf(metaAnnotations);
        packageAnnotations = Set.copyOf(packageAnnotations);
        supertypes = Set.copyOf(supertypes);
    }

    /**
     * @return a public, exposed type without annotations or supertypes
     */
    public static ModuleType of(String name) {
        return new ModuleType(name, true, true, Set.of(), Set.of(), Set.of(), Set.of());
    }

    /**
     * @return whether the type is the given type or one of its subtypes
     */
    public boolean isAssignableTo(String type) {
        return name.equals(type) || supertypes.contains(type);
    }

    /**
     * @return whether the given annotation is declared on the type
     */
    public boolean isAnnotatedWith(String annotation) {
        return annotations.contains(annotation);
    }

    /**
     * @return whether the given annotation is found on one of the type's annotations
     */
    public boolean isMetaAnnotatedWith(String annotation) {
        return metaAnnotations.contains(annotation);
    }

    /**
     * @return whether the type's package, or one of the package's annotations, carries the given annotation
     */
    public boolean isPackageAnnotatedWith(String annotation) {
        return packageAnnotations.contains(annotation);
    }

    /**
     * @return the package of the type, empty for the default package
     */
    public String packageName() {
        return TypeNames.packageOf(name);
    }
}
