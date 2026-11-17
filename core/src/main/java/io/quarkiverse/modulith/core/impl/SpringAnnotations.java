package io.quarkiverse.modulith.core.impl;

import java.util.List;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * Removes Spring Modulith's annotations from a scanned class, for applications that turned their support off.
 */
final class SpringAnnotations {

    private SpringAnnotations() {
    }

    static ScannedClass strip(ScannedClass type) {
        return new ScannedClass(type.name(), type.access(), type.superName(), type.interfaces(), strip(type.annotations()),
                type.fields().stream().map(it -> new ScannedField(it.name(), it.access(), it.type(),
                        strip(it.annotations()), it.typeArguments())).toList(),
                type.methods().stream().map(it -> new ScannedMethod(it.name(), it.descriptor(), it.access(),
                        it.parameterTypes(), it.returnType(), strip(it.annotations()),
                        it.parameterAnnotations().stream().map(SpringAnnotations::strip).toList(), it.involvedTypes(),
                        it.parameterTypeArguments())).toList(),
                type.dependencies(), type.calls(), type.sourceFile(), type.typeArguments());
    }

    private static List<AnnotationData> strip(List<AnnotationData> annotations) {
        return annotations.stream().filter(it -> !it.type().startsWith(Names.SPRING_MODULITH)).toList();
    }
}
