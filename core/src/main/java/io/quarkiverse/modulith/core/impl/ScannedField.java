package io.quarkiverse.modulith.core.impl;

import java.util.List;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * A field of a scanned class.
 *
 * @param name the field name
 * @param access the access flags
 * @param type the binary name of the raw field type
 * @param annotations the annotations of the field
 * @param typeArguments the classes of the field type's type arguments, first level only
 */
record ScannedField(String name, int access, String type, List<AnnotationData> annotations, List<String> typeArguments) {
}
