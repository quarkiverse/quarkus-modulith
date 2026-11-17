package io.quarkiverse.modulith.deployment.docs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Writes class files for types that refer to libraries the deployment tests do not have, such as Jakarta REST or
 * Panache: the index only needs the names.
 */
final class GeneratedClass {

    private final String name;
    private int access = Opcodes.ACC_PUBLIC;
    private String superName = "java/lang/Object";
    private String signature;
    private String[] interfaces = new String[0];
    private final List<String> annotations = new ArrayList<>();
    private final List<Member> fields = new ArrayList<>();
    private final List<Member> methods = new ArrayList<>();
    private final List<Member> abstractMethods = new ArrayList<>();

    private GeneratedClass(String name) {
        this.name = name.replace('.', '/');
    }

    static GeneratedClass named(String name) {
        return new GeneratedClass(name);
    }

    GeneratedClass access(int access) {
        this.access = access;
        return this;
    }

    GeneratedClass extending(String superName) {
        this.superName = superName.replace('.', '/');
        return this;
    }

    GeneratedClass implementing(String signature, String... interfaces) {
        this.signature = signature;
        this.interfaces = List.of(interfaces).stream().map(it -> it.replace('.', '/')).toArray(String[]::new);
        return this;
    }

    GeneratedClass annotatedWith(String... annotations) {
        this.annotations.addAll(List.of(annotations));
        return this;
    }

    GeneratedClass field(String name, String descriptor, String signature, String... annotations) {
        fields.add(new Member(name, descriptor, signature, List.of(annotations), Map.of()));
        return this;
    }

    GeneratedClass method(String name, String descriptor, String... annotations) {
        methods.add(new Member(name, descriptor, null, List.of(annotations), Map.of()));
        return this;
    }

    GeneratedClass abstractMethod(String name, String descriptor) {
        abstractMethods.add(new Member(name, descriptor, null, List.of(), Map.of()));
        return this;
    }

    GeneratedClass methodWithParameterAnnotation(String name, String descriptor, int parameter, String annotation,
            String... annotations) {
        methods.add(new Member(name, descriptor, null, List.of(annotations), Map.of(parameter, annotation)));
        return this;
    }

    byte[] bytes() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V21, access, name, signature, superName, interfaces);
        annotations.forEach(it -> writer.visitAnnotation(descriptor(it), true).visitEnd());
        for (Member field : fields) {
            var visitor = writer.visitField(Opcodes.ACC_PUBLIC, field.name(), field.descriptor(), field.signature(), null);
            field.annotations().forEach(it -> visitor.visitAnnotation(descriptor(it), true).visitEnd());
            visitor.visitEnd();
        }
        for (Member method : methods) {
            MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, method.name(), method.descriptor(), null,
                    null);
            method.annotations().forEach(it -> visitor.visitAnnotation(descriptor(it), true).visitEnd());
            method.parameterAnnotations().forEach((index, annotation) -> {
                AnnotationVisitor parameter = visitor.visitParameterAnnotation(index, descriptor(annotation), true);
                parameter.visitEnd();
            });
            visitor.visitCode();
            visitor.visitInsn(Opcodes.RETURN);
            visitor.visitMaxs(0, 0);
            visitor.visitEnd();
        }
        for (Member method : abstractMethods) {
            writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, method.name(), method.descriptor(), null,
                    null).visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    String className() {
        return name.replace('/', '.');
    }

    private static String descriptor(String annotation) {
        return "L" + annotation.replace('.', '/') + ";";
    }
}
