package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * Reads a class file into a {@link ScannedClass}, collecting the same dependencies ArchUnit's
 * {@code JavaClass#getDirectDependenciesFromSelf()} reports.
 */
final class ScanningClassVisitor extends ClassVisitor {

    private String name;
    private int access;
    private String superName;
    private List<String> interfaces;
    private String sourceFile;
    private DependencyCollector dependencies;
    private final List<AnnotationData> annotations = new ArrayList<>();
    private final List<ScannedField> fields = new ArrayList<>();
    private final List<ScannedMethod> methods = new ArrayList<>();
    private final List<MethodRecord> records = new ArrayList<>();
    private List<String> typeArguments;
    private ScannedClass result;

    ScanningClassVisitor() {
        super(Opcodes.ASM9);
    }

    ScannedClass result() {
        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = TypeNames.internalToBinary(name);
        this.access = access;
        this.superName = Optional.ofNullable(superName).map(TypeNames::internalToBinary).orElse("");
        this.interfaces = Stream.of(interfaces).map(TypeNames::internalToBinary).toList();
        this.dependencies = new DependencyCollector(this.name);
        boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        String origin = (isInterface ? "Interface <" : "Class <") + this.name + ">";
        this.interfaces.forEach(it -> dependencies.addClass(
                origin + (isInterface ? " extends" : " implements") + " interface <" + it + ">", it, 0));
        Optional.ofNullable(superName).map(TypeNames::internalToBinary).ifPresent(
                it -> dependencies.addClass(origin + " extends class <" + it + ">", it, 0));
        String classOrigin = "Class <" + this.name + ">";
        SignatureParts parts = SignatureParts.of(signature);
        parts.superclass().ifPresent(superclass -> superclass.argumentClasses().forEach(it -> dependencies.addClass(
                classOrigin + " has generic superclass <" + superclass.render() + "> with type argument depending on <"
                        + it + ">",
                it, 0)));
        parts.interfaces().forEach(type -> type.argumentClasses().forEach(it -> dependencies.addClass(
                classOrigin + " has generic interface <" + type.render() + "> with type argument depending on <" + it + ">",
                it, 0)));
        typeParameterDependencies(classOrigin, parts);
        typeArguments = Stream.concat(parts.superclass().stream(), parts.interfaces().stream())
                .flatMap(it -> it.arguments().stream().limit(1))
                .filter(it -> !it.variable())
                .map(GenericType::name)
                .toList();
    }

    private void typeParameterDependencies(String origin, SignatureParts parts) {
        parts.typeParameters().forEach((parameter, bounds) -> bounds.forEach(bound -> bound.involvedClasses()
                .forEach(it -> dependencies.addClass(origin + " has type parameter '" + parameter + "' depending on <" + it
                        + ">", it, 0))));
    }

    @Override
    public void visitSource(String source, String debug) {
        this.sourceFile = source;
    }

    /**
     * A nested class keeps its declared modifiers in the inner classes attribute, the class file flags make a
     * protected nested class public.
     */
    @Override
    public void visitInnerClass(String innerName, String outerName, String simpleName, int innerAccess) {
        if (TypeNames.internalToBinary(innerName).equals(name)) {
            access = innerAccess;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return ScanningAnnotationVisitor.declared(descriptor, annotations::add, dependencies, "Class <" + name + ">");
    }

    @Override
    public FieldVisitor visitField(int access, String fieldName, String descriptor, String signature, Object value) {
        String origin = "Field <" + name + "." + fieldName + ">";
        Type type = Type.getType(descriptor);
        dependencies.add(origin, "has type", type, 0);
        Optional<GenericType> generic = SignatureParts.ofType(signature);
        generic.ifPresent(it -> it.argumentClasses().forEach(argument -> dependencies.addClass(origin + " has generic type <"
                + it.render() + "> with type argument depending on <" + argument + ">", argument, 0)));
        return new ScanningFieldVisitor(fieldName, access, TypeNames.name(type),
                generic.stream().flatMap(ScanningClassVisitor::firstLevelArguments).toList(), origin, dependencies,
                fields::add);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
            String[] exceptions) {
        Type[] parameterTypes = Type.getArgumentTypes(descriptor);
        String kind = methodName.equals("<init>") ? "Constructor <"
                : methodName.equals("<clinit>") ? "Static Initializer <" : "Method <";
        String description = kind + name + "." + methodName + "(" + TypeNames.parameterList(descriptor) + ")>";
        MethodRecord record = new MethodRecord(methodName, descriptor, description, parameterTypes.length > 0);
        records.add(record);
        if (record.isSynthetic()) {
            return new ScanningMethodVisitor(record, dependencies, parameterTypes, it -> {
            });
        }
        SignatureParts parts = SignatureParts.of(signature);
        Type returnType = Type.getReturnType(descriptor);
        Stream.of(parameterTypes).forEach(it -> dependencies.add(description, "has parameter of type", it, 0));
        parts.parameters().stream().filter(it -> !it.arguments().isEmpty()).forEach(generic -> generic.involvedClasses()
                .forEach(it -> dependencies.addClass(description + " has generic parameter type <" + generic.render()
                        + "> with type argument depending on <" + it + ">", it, 0)));
        if (!methodName.startsWith("<")) {
            dependencies.add(description, "has return type", returnType, 0);
            parts.returnType().ifPresent(generic -> generic.argumentClasses().forEach(it -> dependencies.addClass(
                    description + " has generic return type <" + generic.render() + "> with type argument depending on <"
                            + it + ">",
                    it, 0)));
        }
        Stream.ofNullable(exceptions).flatMap(Stream::of).map(TypeNames::internalToBinary)
                .forEach(it -> dependencies.addClass(description + " throws type <" + it + ">", it, 0));
        typeParameterDependencies(description, parts);
        List<String> involved = Stream.of(Stream.of(parameterTypes).flatMap(TypeNames::classOf),
                TypeNames.classOf(returnType),
                parts.parameters().stream().flatMap(GenericType::involvedClasses),
                parts.returnType().stream().flatMap(GenericType::involvedClasses))
                .flatMap(Function.identity())
                .distinct()
                .toList();
        List<List<String>> parameterTypeArguments = parts.parameters().size() == parameterTypes.length
                ? parts.parameters().stream().map(it -> firstLevelArguments(it).toList()).toList()
                : Stream.of(parameterTypes).<List<String>> map(it -> List.of()).toList();
        return new ScanningMethodVisitor(record, dependencies, parameterTypes, visitor -> methods.add(new ScannedMethod(
                methodName, descriptor, access, Stream.of(parameterTypes).map(TypeNames::name).toList(),
                TypeNames.name(returnType), List.copyOf(visitor.annotations),
                visitor.parameterAnnotations.stream().map(List::copyOf).toList(), involved, parameterTypeArguments)));
    }

    private static Stream<String> firstLevelArguments(GenericType type) {
        return type.arguments().stream().filter(it -> !it.variable() && it.wildcard() == ' ').map(GenericType::name);
    }

    @Override
    public void visitEnd() {
        Map<String, MethodRecord> byKey = new HashMap<>();
        records.forEach(it -> byKey.put(it.key, it));
        Set<String> attributed = new HashSet<>();
        List<ScannedCall> calls = new ArrayList<>();
        records.stream().filter(it -> !it.isSynthetic())
                .forEach(it -> attribute(it, it, byKey, attributed, calls));
        records.stream().filter(it -> !attributed.contains(it.key))
                .forEach(it -> attribute(it, it, byKey, attributed, calls));
        String file = Objects.requireNonNullElseGet(sourceFile,
                () -> name.substring(name.lastIndexOf('.') + 1).split("\\$")[0] + ".java");
        result = new ScannedClass(name, access, superName, interfaces, List.copyOf(annotations), List.copyOf(fields),
                List.copyOf(methods), dependencies.finish(file), List.copyOf(calls), file,
                typeArguments);
    }

    private void attribute(MethodRecord owner, MethodRecord record, Map<String, MethodRecord> byKey, Set<String> attributed,
            List<ScannedCall> calls) {
        attributed.add(record.key);
        record.accesses.forEach(it -> dependencies.add(owner.description, it.verb(), it.target(), it.targetDescription(),
                it.line()));
        record.calls.forEach(it -> calls.add(new ScannedCall(owner.name, owner.hasParameters, it.owner(), it.name(),
                it.descriptor())));
        record.lambdas.stream().map(byKey::get).filter(Objects::nonNull).filter(it -> !attributed.contains(it.key))
                .forEach(it -> attribute(owner, it, byKey, attributed, calls));
    }
}
