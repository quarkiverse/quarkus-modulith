package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * Reads the annotations of a field.
 */
final class ScanningFieldVisitor extends FieldVisitor {

    private final String name;
    private final int access;
    private final String type;
    private final String origin;
    private final DependencyCollector dependencies;
    private final Consumer<ScannedField> sink;
    private final List<String> typeArguments;
    private final List<AnnotationData> annotations = new ArrayList<>();

    ScanningFieldVisitor(String name, int access, String type, List<String> typeArguments, String origin,
            DependencyCollector dependencies, Consumer<ScannedField> sink) {
        super(Opcodes.ASM9);
        this.name = name;
        this.access = access;
        this.type = type;
        this.origin = origin;
        this.dependencies = dependencies;
        this.sink = sink;
        this.typeArguments = typeArguments;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return ScanningAnnotationVisitor.declared(descriptor, annotations::add, dependencies, origin);
    }

    @Override
    public void visitEnd() {
        sink.accept(new ScannedField(name, access, type, List.copyOf(annotations), typeArguments));
    }
}
