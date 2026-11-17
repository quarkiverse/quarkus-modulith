package io.quarkiverse.modulith.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class EdgeCasesTest {

    static byte[] generated() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "a/b/Gen$Part", null, "java/lang/Object", null);
        MethodVisitor orphan = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                "lambda$gone$0", "()V", null, null);
        orphan.visitCode();
        orphan.visitFieldInsn(Opcodes.PUTSTATIC, "c/d/Target", "value", "I");
        orphan.visitInsn(Opcodes.RETURN);
        orphan.visitMaxs(1, 0);
        orphan.visitEnd();
        Handle lambdaFactory = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "()V", false);
        Handle lambda = new Handle(Opcodes.H_INVOKESTATIC, "a/b/Gen$Part", "lambda$shared$1", "()V", false);
        for (String name : new String[] { "first", "second" }) {
            MethodVisitor user = writer.visitMethod(Opcodes.ACC_PUBLIC, name, "()V", null, null);
            user.visitCode();
            user.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", lambdaFactory, Type.getType("()V"), lambda,
                    Type.getType("()V"));
            user.visitInsn(Opcodes.RETURN);
            user.visitMaxs(1, 1);
            user.visitEnd();
        }
        MethodVisitor shared = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                "lambda$shared$1", "()V", null, null);
        shared.visitCode();
        shared.visitFieldInsn(Opcodes.GETSTATIC, "c/d/Shared", "value", "I");
        shared.visitInsn(Opcodes.RETURN);
        shared.visitMaxs(1, 0);
        shared.visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.visitCode();
        method.visitLdcInsn(Type.getMethodType("()V"));
        method.visitLdcInsn("text");
        Handle metafactory = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "()V", false);
        method.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", metafactory, Type.getType("()V"), "notAHandle",
                Type.getType("()V"));
        Handle other = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/StringConcatFactory", "makeConcat", "()V",
                false);
        method.visitInvokeDynamicInsn("concat", "()Ljava/lang/String;", other, new Handle(Opcodes.H_INVOKESTATIC,
                "c/d/Target", "make", "()V", false), Type.getType("()V"));
        method.visitTypeInsn(Opcodes.NEW, "c/d/Target");
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(4, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    @Test
    void scansClassesWithoutDebugInformation() {
        // act
        ScannedClass scanned = ClassFileScanner.scan(generated());

        // assert
        assertThat(scanned.sourceFile()).isEqualTo("Gen.java");
        assertThat(scanned.methods()).extracting(ScannedMethod::name).containsExactly("first", "second", "run");
        assertThat(scanned.dependencies()).extracting(RawDependency::description).containsExactly(
                "Method <a.b.Gen$Part.first()> gets field <c.d.Shared.value> in (Gen.java:0)",
                "Method <a.b.Gen$Part.lambda$gone$0()> sets field <c.d.Target.value> in (Gen.java:0)");
    }

    @Test
    void reportsUnreadablePropertiesFiles() {
        // arrange
        Properties properties = new Properties();

        // act & assert
        assertThatThrownBy(() -> ModulithProperties.read(properties, new BrokenStream()))
                .isInstanceOf(UncheckedIOException.class).hasMessage("java.io.IOException: broken");
    }

    @Test
    void readsNoPropertiesFromAnUnreadableFile() {
        // arrange
        Properties properties = new Properties();

        // act
        catchThrowable(() -> ModulithProperties.read(properties, new BrokenStream()));

        // assert
        assertThat(properties).isEmpty();
    }

    @Test
    void reportsFailingResourceLookups() {
        // arrange
        ClassLoader classLoader = new FailingClassLoader();

        // act & assert
        assertThatThrownBy(() -> ClassPathFiles.resources(classLoader, "a"))
                .isInstanceOf(UncheckedIOException.class).hasMessage("java.io.IOException: no resources");
    }

    @Test
    void reportsMissingDirectories() throws MalformedURLException {
        // arrange
        URL location = URI.create("file:/does/not/exist").toURL();

        // act & assert
        assertThatThrownBy(() -> ClassPathFiles.classes(location, "a"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Cannot read the classes of file:/does/not/exist");
    }

    @Test
    void reportsMissingJars() throws MalformedURLException {
        // arrange
        URL location = URI.create("jar:file:/does/not/exist.jar!/a").toURL();

        // act & assert
        assertThatThrownBy(() -> ClassPathFiles.classes(location, "a")).isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void rejectsLocationsThatAreNeitherDirectoriesNorJars() throws MalformedURLException {
        // arrange
        URL location = URI.create("jrt:/java.base/java/lang").toURL();

        // act & assert
        assertThatThrownBy(() -> ClassPathFiles.classes(location, "java/lang"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot read the classes of jrt:/java.base/java/lang: only directories and jars are supported");
    }

    @Test
    void reportsMissingClassFiles() {
        // arrange
        URI location = URI.create("file:/does/not/exist.class");

        // act & assert
        assertThatThrownBy(() -> ClassPathFiles.bytes(location)).isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(FileNotFoundException.class);
    }
}
