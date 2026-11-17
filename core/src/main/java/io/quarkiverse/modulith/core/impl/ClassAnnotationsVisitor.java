package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Collects the names of the annotation types on a class, and nothing else.
 */
final class ClassAnnotationsVisitor extends ClassVisitor {

    private final List<String> annotations = new ArrayList<>();

    ClassAnnotationsVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        annotations.add(Type.getType(descriptor).getClassName());
        return null;
    }

    List<String> annotations() {
        return List.copyOf(annotations);
    }
}
