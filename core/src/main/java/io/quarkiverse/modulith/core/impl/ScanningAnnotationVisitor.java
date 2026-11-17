package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.AttributeData;

/**
 * Reads the attributes of an annotation as text and records the types its members refer to. An instance either reads
 * a whole annotation, or the elements of one array attribute for its parent.
 */
final class ScanningAnnotationVisitor extends AnnotationVisitor {

    private final String type;
    private final Consumer<AnnotationData> sink;
    private final DependencyCollector dependencies;
    private final String origin;
    private final List<AttributeData> attributes = new ArrayList<>();
    private final ScanningAnnotationVisitor parent;
    private final String arrayName;
    private final List<String> arrayValues = new ArrayList<>();

    private ScanningAnnotationVisitor(String type, Consumer<AnnotationData> sink, DependencyCollector dependencies,
            String origin, ScanningAnnotationVisitor parent, String arrayName) {
        super(Opcodes.ASM9);
        this.type = type;
        this.sink = sink;
        this.dependencies = dependencies;
        this.origin = origin;
        this.parent = parent;
        this.arrayName = arrayName;
    }

    /**
     * Visits an annotation declared on the element described by {@code origin}, recording the dependency on the
     * annotation type.
     */
    static AnnotationVisitor declared(String descriptor, Consumer<AnnotationData> sink, DependencyCollector dependencies,
            String origin) {
        Type annotationType = Type.getType(descriptor);
        dependencies.add(origin, "is annotated with", annotationType, 0);
        return new ScanningAnnotationVisitor(annotationType.getClassName(), sink, dependencies, origin, null, "");
    }

    @Override
    public void visit(String name, Object value) {
        add(name, value instanceof Type memberType ? memberType(memberType) : String.valueOf(value));
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        memberType(Type.getType(descriptor));
        add(name, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        add(name, memberType(Type.getType(descriptor)));
        return new ScanningAnnotationVisitor(Type.getType(descriptor).getClassName(), it -> {
        }, dependencies, origin, null, "");
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return new ScanningAnnotationVisitor(type, sink, dependencies, origin, this, name);
    }

    private String memberType(Type memberType) {
        dependencies.add(origin, "has annotation member of type", memberType, 0);
        return TypeNames.name(memberType);
    }

    private void add(String name, String value) {
        if (Objects.isNull(parent)) {
            attributes.add(new AttributeData(name, List.of(value)));
        } else {
            arrayValues.add(value);
        }
    }

    @Override
    public void visitEnd() {
        if (Objects.isNull(parent)) {
            sink.accept(new AnnotationData(type, List.copyOf(attributes)));
        } else {
            parent.attributes.add(new AttributeData(arrayName, List.copyOf(arrayValues)));
        }
    }
}
