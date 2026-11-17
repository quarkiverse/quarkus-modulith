package io.quarkiverse.modulith.core.impl;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

/**
 * Converts ASM types to the names ArchUnit and {@link Class#getName()} use.
 */
final class TypeNames {

    private TypeNames() {
    }

    /**
     * @return {@code com.acme.Order}, {@code [Lcom.acme.Order;} or {@code int}
     */
    static String name(Type type) {
        return type.getSort() == Type.ARRAY ? type.getDescriptor().replace('/', '.') : type.getClassName();
    }

    static String internalToBinary(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * @return the class a type refers to, the element class for arrays, nothing for primitives
     */
    static Stream<String> classOf(Type type) {
        Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
        return element.getSort() == Type.OBJECT ? Stream.of(element.getClassName()) : Stream.empty();
    }

    /**
     * @return whether the name belongs to the JDK, which the module analysis ignores like Spring Modulith does
     */
    static boolean isCoreType(String name) {
        return name.startsWith("java.") || name.startsWith("javax.");
    }

    /**
     * @return the parameter type names of a method descriptor, separated by commas
     */
    static String parameterList(String descriptor) {
        return Arrays.stream(Type.getArgumentTypes(descriptor)).map(TypeNames::name).collect(Collectors.joining(", "));
    }
}
