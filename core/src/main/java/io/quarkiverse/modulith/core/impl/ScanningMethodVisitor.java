package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * Reads the annotations of a method and the references its instructions make.
 */
final class ScanningMethodVisitor extends MethodVisitor {

    private static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";

    private final MethodRecord record;
    private final DependencyCollector dependencies;
    private final Type[] parameterTypes;
    private final Consumer<ScanningMethodVisitor> sink;
    final List<AnnotationData> annotations = new ArrayList<>();
    final List<List<AnnotationData>> parameterAnnotations;
    private int line;
    private int offset;

    ScanningMethodVisitor(MethodRecord record, DependencyCollector dependencies, Type[] parameterTypes,
            Consumer<ScanningMethodVisitor> sink) {
        super(Opcodes.ASM9);
        this.record = record;
        this.dependencies = dependencies;
        this.parameterTypes = parameterTypes;
        this.sink = sink;
        this.parameterAnnotations = IntStream.range(0, parameterTypes.length)
                .<List<AnnotationData>> mapToObj(it -> new ArrayList<>())
                .toList();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return ScanningAnnotationVisitor.declared(descriptor, annotations::add, dependencies, record.description);
    }

    /**
     * Synthetic parameters, such as the outer instance of an inner class constructor, come first and carry no
     * annotations.
     */
    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        offset = parameterTypes.length - parameterCount;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        int index = parameter + offset;
        String origin = "Parameter <" + TypeNames.name(parameterTypes[index]) + "> of "
                + Character.toLowerCase(record.description.charAt(0)) + record.description.substring(1);
        return ScanningAnnotationVisitor.declared(descriptor, parameterAnnotations.get(index)::add, dependencies, origin);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.line = line;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        boolean get = opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC;
        record.accesses.add(new PendingAccess(get ? "gets" : "sets", Type.getObjectType(owner),
                "field <" + TypeNames.internalToBinary(owner) + "." + name + ">", line));
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        Type ownerType = Type.getObjectType(owner);
        record.accesses.add(new PendingAccess("calls", ownerType, member(ownerType, name, descriptor), line));
        record.calls.add(new ScannedCall("", false, TypeNames.name(ownerType), name, descriptor));
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.INSTANCEOF) {
            Type checked = Type.getObjectType(type);
            record.accesses.add(new PendingAccess("checks instanceof", checked, "<" + TypeNames.name(checked) + ">", line));
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type type && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
            record.accesses.add(new PendingAccess("references class object", type, "<" + TypeNames.name(type) + ">", line));
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments) {
        if (bootstrapMethodHandle.getOwner().equals(LAMBDA_METAFACTORY)
                && bootstrapMethodArguments[1] instanceof Handle handle) {
            if (MethodRecord.isSynthetic(handle.getName())) {
                record.lambdas.add(handle.getName() + handle.getDesc());
            } else {
                Type ownerType = Type.getObjectType(handle.getOwner());
                record.accesses.add(new PendingAccess("references", ownerType,
                        member(ownerType, handle.getName(), handle.getDesc()), line));
            }
        }
    }

    private static String member(Type owner, String name, String descriptor) {
        return (name.equals("<init>") ? "constructor <" : "method <") + TypeNames.name(owner) + "." + name + "("
                + TypeNames.parameterList(descriptor) + ")>";
    }

    @Override
    public void visitEnd() {
        sink.accept(this);
    }
}
