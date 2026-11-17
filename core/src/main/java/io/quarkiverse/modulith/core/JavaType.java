package io.quarkiverse.modulith.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.TypeData;

/**
 * A type of the application, the counterpart of ArchUnit's {@code JavaClass} in Spring Modulith's API. It is backed by
 * data computed when the model was built, so it is available in the running application without class path scanning.
 */
public final class JavaType implements Comparable<JavaType> {

    static final int ANNOTATION = 0x2000;
    static final int ENUM = 0x4000;
    static final int INTERFACE = 0x0200;

    private final TypeData data;

    JavaType(TypeData data) {
        this.data = data;
    }

    /**
     * @param data the recorded type
     * @return the type
     */
    public static JavaType of(TypeData data) {
        return new JavaType(data);
    }

    /**
     * @return the binary name, {@code com.acme.Order$Line} for a nested type
     */
    public String getName() {
        return data.name();
    }

    /**
     * @return same as {@link #getName()}, under ArchUnit's name
     */
    public String getFullName() {
        return data.name();
    }

    /**
     * @return the simple name, {@code Line} for {@code com.acme.Order$Line}
     */
    public String getSimpleName() {
        String name = data.name();
        return name.substring(Math.max(name.lastIndexOf('.'), name.lastIndexOf('$')) + 1);
    }

    /**
     * @return the name of the package, empty for the default package
     */
    public String getPackageName() {
        return PackageName.packageOf(data.name());
    }

    /**
     * @return the access flags of the type
     */
    public int getModifiers() {
        return data.modifiers();
    }

    public boolean isPublic() {
        return Modifier.isPublic(data.modifiers());
    }

    public boolean isInterface() {
        return (data.modifiers() & INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(data.modifiers());
    }

    public boolean isAnnotation() {
        return (data.modifiers() & ANNOTATION) != 0;
    }

    public boolean isEnum() {
        return (data.modifiers() & ENUM) != 0;
    }

    /**
     * @return whether the type is a {@code package-info} type
     */
    public boolean isPackageInfo() {
        return getSimpleName().equals("package-info");
    }

    /**
     * @return whether the type declares the annotation with the given name
     */
    public boolean isAnnotatedWith(String annotation) {
        return data.annotations().contains(annotation);
    }

    public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
        return isAnnotatedWith(annotation.getName());
    }

    /**
     * @return whether the type declares the annotation, or an annotation that is (meta-)annotated with it
     */
    public boolean isMetaAnnotatedWith(String annotation) {
        return isAnnotatedWith(annotation) || data.metaAnnotations().contains(annotation);
    }

    public boolean isMetaAnnotatedWith(Class<? extends Annotation> annotation) {
        return isMetaAnnotatedWith(annotation.getName());
    }

    /**
     * @return whether the type is the given type or one of its subtypes
     */
    public boolean isAssignableTo(String type) {
        return data.name().equals(type) || data.supertypes().contains(type);
    }

    public boolean isAssignableTo(Class<?> type) {
        return isAssignableTo(type.getName());
    }

    /**
     * @return the names of the interfaces the type implements directly
     */
    public List<String> getInterfaceNames() {
        return data.interfaces();
    }

    /**
     * @return the names of all supertypes, transitively
     */
    public List<String> getSupertypeNames() {
        return data.supertypes();
    }

    /**
     * @return the names of the declared annotations
     */
    public List<String> getAnnotationNames() {
        return data.annotations();
    }

    /**
     * @return the names of the annotations present on the declared annotations, transitively
     */
    public List<String> getMetaAnnotationNames() {
        return data.metaAnnotations();
    }

    /**
     * Loads the type with the context class loader, without initializing it. In a native executable only the types
     * registered for reflection can be loaded by name; the module model itself works without loading any type.
     *
     * @throws IllegalStateException if the type cannot be loaded
     */
    public Class<?> reflect() {
        try {
            return Class.forName(data.name(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load " + data.name(), e);
        }
    }

    TypeData getData() {
        return data;
    }

    List<AnnotationData> getDeclarations() {
        return data.declarations();
    }

    boolean hasStereotype(String stereotype) {
        return data.stereotypes().contains(stereotype);
    }

    @Override
    public int compareTo(JavaType other) {
        return data.name().compareTo(other.data.name());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaType that && data.name().equals(that.data.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(data.name());
    }

    @Override
    public String toString() {
        return data.name();
    }
}
