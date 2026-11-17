package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.objectweb.asm.Opcodes;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * A method or constructor of a scanned class.
 *
 * @param name the method name, {@code <init>} for constructors
 * @param descriptor the method descriptor
 * @param access the access flags
 * @param parameterTypes the binary names of the raw parameter types
 * @param returnType the binary name of the raw return type, {@code void} for none
 * @param annotations the annotations of the method
 * @param parameterAnnotations the annotations of each parameter
 * @param involvedTypes the classes of the parameter and return types, generic type arguments and array components
 *        included
 * @param parameterTypeArguments the classes of each generic parameter type's type arguments, first level only
 */
record ScannedMethod(String name, String descriptor, int access, List<String> parameterTypes, String returnType,
        List<AnnotationData> annotations, List<List<AnnotationData>> parameterAnnotations, List<String> involvedTypes,
        List<List<String>> parameterTypeArguments) {

    boolean isConstructor() {
        return name.equals("<init>");
    }

    boolean isStatic() {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    boolean isPublic() {
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    boolean isSynthetic() {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    /**
     * @return the first annotation of one of the given types
     */
    Optional<AnnotationData> annotation(Set<String> types) {
        return annotations.stream().filter(it -> types.contains(it.type())).findFirst();
    }

    boolean hasAnnotation(Set<String> types) {
        return annotation(types).isPresent();
    }

    /**
     * @return whether a parameter carries one of the given annotations
     */
    boolean hasParameterAnnotation(Set<String> types) {
        return parameterAnnotations.stream().flatMap(List::stream).anyMatch(it -> types.contains(it.type()));
    }
}
