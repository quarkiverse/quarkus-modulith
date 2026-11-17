package io.quarkiverse.modulith.core.impl;

import java.util.List;

import org.objectweb.asm.Opcodes;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * What the scanner reads from a class file.
 *
 * @param name the binary name
 * @param access the access flags
 * @param superName the binary name of the superclass, empty for {@code java.lang.Object} and interfaces without
 *        superclass
 * @param interfaces the binary names of the implemented interfaces
 * @param annotations the annotations of the class
 * @param fields the fields
 * @param methods the methods and constructors, without synthetic lambda and accessor methods
 * @param dependencies the references to other classes, as ArchUnit reports them
 * @param calls the method and constructor calls
 * @param sourceFile the name of the source file
 * @param typeArguments the first type argument of each generic supertype that is a class, for example the domain
 *        type of a repository
 */
record ScannedClass(String name, int access, String superName, List<String> interfaces, List<AnnotationData> annotations,
        List<ScannedField> fields, List<ScannedMethod> methods, List<RawDependency> dependencies, List<ScannedCall> calls,
        String sourceFile, List<String> typeArguments) {

    boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }
}
