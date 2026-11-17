package io.quarkiverse.modulith.core.data;

import java.util.List;

/**
 * A type of the application as seen by the module analysis.
 *
 * @param name the binary name of the type ({@code com.acme.Order$Line} for a nested type)
 * @param modifiers the access flags of the class file ({@link java.lang.reflect.Modifier} bits, plus
 *        {@code 0x2000} for annotations and {@code 0x4000} for enums)
 * @param supertypes the names of all supertypes, transitively, without {@code java.lang.Object}
 * @param interfaces the names of the directly implemented interfaces
 * @param annotations the names of the annotations declared on the type
 * @param metaAnnotations the names of the annotations present on the declared annotations, transitively
 * @param signatureTypes the application types that appear in the public constructors and methods of the type
 * @param declarations the module declarations found on the type
 * @param stereotypes the architectural stereotypes of the type, names of {@code ArchitecturallyEvidentType} queries
 * @param referenceMethods the event listener methods of the type
 */
public record TypeData(String name, int modifiers, List<String> supertypes, List<String> interfaces,
        List<String> annotations, List<String> metaAnnotations, List<String> signatureTypes,
        List<AnnotationData> declarations, List<String> stereotypes, List<ReferenceMethodData> referenceMethods) {

    /**
     * @return a copy with the given stereotypes and listener methods
     */
    public TypeData withStereotypes(List<String> stereotypes, List<ReferenceMethodData> referenceMethods) {
        return new TypeData(name, modifiers, supertypes, interfaces, annotations, metaAnnotations, signatureTypes,
                declarations, List.copyOf(stereotypes), List.copyOf(referenceMethods));
    }
}
