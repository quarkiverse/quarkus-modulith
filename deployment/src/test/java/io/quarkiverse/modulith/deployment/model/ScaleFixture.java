package io.quarkiverse.modulith.deployment.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates a large application: {@link #MODULES} modules of {@link #TYPES_PER_MODULE} class files each.
 * Every module exposes services in its base package and ports in a named interface {@code spi}, and keeps the rest in
 * {@code internal}. The internal types of module {@code n} use the services of module {@code n - 1}, the ports of
 * module {@code n - 2}, and observe the event of module {@code n - 1}, so the modules form a chain without cycles.
 */
public final class ScaleFixture {

    public static final String ROOT = "io.quarkiverse.modulith.deployment.fixtures.model.scale";

    public static final int MODULES = 20;

    public static final int SERVICES = 10;

    public static final int PORTS = 5;

    public static final int INTERNALS = 83;

    public static final int TYPES_PER_MODULE = SERVICES + PORTS + INTERNALS + 2;

    private static final String OBJECT = "java/lang/Object";

    private static final String SCOPE = "Ljakarta/enterprise/context/ApplicationScoped;";

    private ScaleFixture() {
    }

    /**
     * @return the class files by binary name
     */
    public static Map<String, byte[]> classes() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        IntStream.range(0, MODULES).forEach(module -> module(module, classes));
        return classes;
    }

    /**
     * @return the identifier of the module with the given index
     */
    public static String module(int index) {
        return "m%02d".formatted(index);
    }

    private static void module(int module, Map<String, byte[]> classes) {
        IntStream.range(0, SERVICES).forEach(index -> put(classes, service(module, index)));
        put(classes, event(module));
        put(classes, spiPackage(module));
        IntStream.range(0, PORTS).forEach(index -> put(classes, port(module, index)));
        IntStream.range(0, INTERNALS).forEach(index -> put(classes, internal(module, index)));
    }

    private static void put(Map<String, byte[]> classes, ClassWriter writer) {
        byte[] bytes = writer.toByteArray();
        classes.put(new ClassReader(bytes).getClassName().replace('/', '.'), bytes);
    }

    private static String base(int module) {
        return (ROOT + "." + module(module)).replace('.', '/');
    }

    private static String serviceName(int module, int index) {
        return base(module) + "/Service" + index;
    }

    private static String eventName(int module) {
        return base(module) + "/Changed";
    }

    private static String portName(int module, int index) {
        return base(module) + "/spi/Port" + index;
    }

    private static String internalName(int module, int index) {
        return base(module) + "/internal/Impl" + index;
    }

    private static ClassWriter service(int module, int index) {
        ClassWriter writer = type(serviceName(module, index), Opcodes.ACC_PUBLIC);
        if (index < 3) {
            writer.visitAnnotation(SCOPE, true).visitEnd();
        }
        MethodVisitor work = writer.visitMethod(Opcodes.ACC_PUBLIC, "work", "(I)I", null, null);
        work.visitCode();
        work.visitVarInsn(Opcodes.ILOAD, 1);
        work.visitInsn(Opcodes.IRETURN);
        work.visitMaxs(1, 2);
        work.visitEnd();
        return done(writer);
    }

    private static ClassWriter event(int module) {
        return done(type(eventName(module), Opcodes.ACC_PUBLIC));
    }

    private static ClassWriter spiPackage(int module) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC,
                base(module) + "/spi/package-info", null, OBJECT, null);
        writer.visitAnnotation("Lio/quarkiverse/modulith/NamedInterface;", true).visitEnd();
        writer.visitEnd();
        return writer;
    }

    private static ClassWriter port(int module, int index) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                portName(module, index), null, OBJECT, null);
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "port", "()I", null, null).visitEnd();
        writer.visitEnd();
        return writer;
    }

    private static ClassWriter internal(int module, int index) {
        String name = internalName(module, index);
        ClassWriter writer = type(name, 0);
        String next = internalName(module, (index + 1) % INTERNALS);
        writer.visitField(0, "next", "L" + next + ";", null, null).visitEnd();
        if (module > 0) {
            serviceUse(writer, name, serviceName(module - 1, index % SERVICES));
        }
        if (module > 1) {
            portUse(writer, name, portName(module - 2, index % PORTS));
        }
        if (index == 0) {
            publisher(writer, eventName(module));
        }
        if (index == 1 && module > 0) {
            writer.visitAnnotation(SCOPE, true).visitEnd();
            observer(writer, eventName(module - 1));
        }
        return done(writer);
    }

    private static void serviceUse(ClassWriter writer, String owner, String service) {
        writer.visitField(0, "service", "L" + service + ";", null, null).visitEnd();
        MethodVisitor run = writer.visitMethod(Opcodes.ACC_PUBLIC, "run", "(I)I", null, null);
        run.visitCode();
        run.visitVarInsn(Opcodes.ALOAD, 0);
        run.visitFieldInsn(Opcodes.GETFIELD, owner, "service", "L" + service + ";");
        run.visitVarInsn(Opcodes.ILOAD, 1);
        run.visitMethodInsn(Opcodes.INVOKEVIRTUAL, service, "work", "(I)I", false);
        run.visitInsn(Opcodes.IRETURN);
        run.visitMaxs(2, 2);
        run.visitEnd();
    }

    private static void portUse(ClassWriter writer, String owner, String port) {
        MethodVisitor use = writer.visitMethod(Opcodes.ACC_PUBLIC, "use", "(L" + port + ";)Ljava/lang/Object;", null,
                null);
        use.visitCode();
        use.visitVarInsn(Opcodes.ALOAD, 1);
        use.visitMethodInsn(Opcodes.INVOKEINTERFACE, port, "port", "()I", true);
        use.visitInsn(Opcodes.POP);
        use.visitLdcInsn(Type.getObjectType(owner));
        use.visitInsn(Opcodes.ARETURN);
        use.visitMaxs(1, 2);
        use.visitEnd();
    }

    private static void publisher(ClassWriter writer, String event) {
        MethodVisitor publish = writer.visitMethod(Opcodes.ACC_PUBLIC, "publish", "()Ljava/lang/Object;", null, null);
        publish.visitCode();
        publish.visitTypeInsn(Opcodes.NEW, event);
        publish.visitInsn(Opcodes.DUP);
        publish.visitMethodInsn(Opcodes.INVOKESPECIAL, event, "<init>", "()V", false);
        publish.visitInsn(Opcodes.ARETURN);
        publish.visitMaxs(2, 1);
        publish.visitEnd();
    }

    private static void observer(ClassWriter writer, String event) {
        MethodVisitor on = writer.visitMethod(0, "on", "(L" + event + ";)V", null, null);
        on.visitParameterAnnotation(0, "Ljakarta/enterprise/event/Observes;", true).visitEnd();
        on.visitCode();
        on.visitInsn(Opcodes.RETURN);
        on.visitMaxs(0, 2);
        on.visitEnd();
    }

    private static ClassWriter type(String name, int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, access | Opcodes.ACC_SUPER, name, null, OBJECT, null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        return writer;
    }

    private static ClassWriter done(ClassWriter writer) {
        writer.visitEnd();
        return writer;
    }
}
