package io.quarkiverse.modulith.core;

import java.lang.reflect.Modifier;
import java.util.List;

import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.AttributeData;
import io.quarkiverse.modulith.core.data.TypeData;

/**
 * Builds types for the value type tests.
 */
final class Types {

    private Types() {
    }

    static JavaType publicType(String name, AnnotationData... declarations) {
        return JavaType.of(new TypeData(name, Modifier.PUBLIC, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(declarations), List.of(), List.of()));
    }

    static JavaType marker(String name, AnnotationData... declarations) {
        return JavaType.of(new TypeData(name, Modifier.PUBLIC, List.of(), List.of(),
                List.of("io.quarkiverse.modulith.PackageInfo"), List.of(), List.of(), List.of(declarations), List.of(),
                List.of()));
    }

    static JavaType hiddenType(String name) {
        return JavaType.of(new TypeData(name, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of()));
    }

    static JavaType packageInfo(String packageName, AnnotationData... declarations) {
        return JavaType.of(new TypeData(packageName + ".package-info", 0x1600, List.of(), List.of(),
                List.of(declarations).stream().map(AnnotationData::type).toList(), List.of(), List.of(),
                List.of(declarations), List.of(), List.of()));
    }

    static AnnotationData namedInterface(String... names) {
        return new AnnotationData("io.quarkiverse.modulith.NamedInterface",
                names.length == 0 ? List.of() : List.of(new AttributeData("value", List.of(names))));
    }
}
