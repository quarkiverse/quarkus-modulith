package io.quarkiverse.modulith.core.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.fixtures.scan.Scanned;
import io.quarkiverse.modulith.core.fixtures.scan.Target;

class ClassFileScannerTest {

    static final String S = Scanned.class.getName();
    static final String T = Target.class.getName();

    static ScannedClass scan(Class<?> type) {
        return scan(type.getName());
    }

    static ScannedClass scan(String name) {
        return ClassFileScanner.scan(ModelAnalyzer.readAll(ClassFileScannerTest.class.getClassLoader()
                .getResourceAsStream(name.replace('.', '/') + ".class")));
    }

    static List<String> descriptions(Class<?> type) {
        return scan(type).dependencies().stream().map(RawDependency::description).toList();
    }

    @Test
    void recordsDeclarationDependenciesLikeArchUnit() {
        // act
        List<String> descriptions = descriptions(Scanned.class);

        // assert
        assertThat(descriptions).contains(
                "Class <" + S + "> has generic superclass <java.util.ArrayList<" + T + "$Inner<java.lang.String>>> with"
                        + " type argument depending on <" + T + "$Inner> in (Scanned.java:0)",
                "Class <" + S + "> has type parameter 'T' depending on <" + T + "> in (Scanned.java:0)",
                "Class <" + S + "> is annotated with <io.quarkiverse.modulith.core.fixtures.scan.Marker> in (Scanned.java:0)",
                "Class <" + S + "> has annotation member of type <" + T + "> in (Scanned.java:0)",
                "Class <" + S + "> depends on component type <" + T + "> in (Scanned.java:0)",
                "Field <" + S + ".matrix> depends on component type <" + T + "> in (Scanned.java:0)",
                "Field <" + S + ".wildcards> has generic type <java.util.Map<? super " + T
                        + ", ? extends java.util.List<?>>> with type argument depending on <" + T + "> in (Scanned.java:0)",
                "Field <" + S + ".inner> has generic type <" + T + "$Inner<" + T + ">> with type argument depending on <"
                        + T + "> in (Scanned.java:0)",
                "Method <" + S + ".generic(java.util.List)> has type parameter 'X' depending on <" + T
                        + "> in (Scanned.java:0)",
                "Method <" + S + ".generic(java.util.List)> has return type <" + T + "> in (Scanned.java:0)",
                "Method <" + S + ".annotated(" + T + ", int)> has parameter of type <" + T + "> in (Scanned.java:0)");
        assertThat(descriptions).noneMatch(it -> it.contains("java.io.IOException") || it.contains("Thread$State"));
    }

    @Test
    void recordsInstructionDependenciesOfLambdasForTheirMethod() {
        // act
        List<String> descriptions = descriptions(Scanned.class);

        // assert
        String origin = "Method <" + S + ".references(java.lang.Object)> ";
        assertThat(descriptions).contains(
                origin + "checks instanceof <" + T + "> in (Scanned.java:26)",
                origin + "references class object <" + T + "> in (Scanned.java:27)",
                origin + "depends on component type <" + T + "> in (Scanned.java:28)",
                origin + "references constructor <" + T + ".<init>()> in (Scanned.java:29)",
                origin + "references method <" + T + ".create()> in (Scanned.java:30)",
                origin + "calls method <" + T + ".create()> in (Scanned.java:32)",
                origin + "calls method <" + T + ".run()> in (Scanned.java:32)");
        assertThat(scan(Scanned.class).calls()).extracting(ScannedCall::originMethod).contains("references")
                .doesNotContain("lambda$references$0");
    }

    @Test
    void readsAnnotationValuesAndMembers() {
        // act
        ScannedClass scanned = scan(Scanned.class);

        // assert
        assertThat(scanned.annotations()).extracting(AnnotationData::type)
                .containsExactly("io.quarkiverse.modulith.core.fixtures.scan.Marker");
        assertThat(scanned.annotations().get(0).values("types")).hasValue(List.of(T, "[L" + T + ";"));
        assertThat(scanned.annotations().get(0).values("nested")).hasValue(List.of("java.lang.Deprecated"));
        assertThat(scanned.annotations().get(0).values("states")).hasValue(List.of("NEW"));
        assertThat(scanned.annotations().get(0).values("many")).hasValue(List.of("java.lang.Deprecated",
                "java.lang.Deprecated"));
        assertThat(scanned.methods()).filteredOn(it -> it.name().equals("annotated")).singleElement()
                .satisfies(it -> assertThat(it.parameterAnnotations()).extracting(List::size).containsExactly(1, 0));
        assertThat(scan(S + "$Member").methods()).filteredOn(ScannedMethod::isConstructor).singleElement()
                .satisfies(it -> assertThat(it.parameterAnnotations()).extracting(List::size).containsExactly(0, 1));
    }

    @Test
    void keepsTheDeclaredModifiersOfNestedClasses() {
        // act
        ScannedClass nested = scan(T + "$Protected");

        // assert
        assertThat(nested.access() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)).isEqualTo(Opcodes.ACC_PROTECTED);
    }

    @Test
    void keepsTheModifiersOfTopLevelClasses() {
        // act
        ScannedClass target = scan(Target.class);

        // assert
        assertThat(target.access() & Opcodes.ACC_PUBLIC).isEqualTo(Opcodes.ACC_PUBLIC);
    }

    @Test
    void givesObjectNoSuperclass() {
        // act
        ScannedClass object = scan(Object.class);

        // assert
        assertThat(object.superName()).isEmpty();
    }

    @Test
    void recordsNoTypeArgumentsForPlainSupertypes() {
        // act
        ScannedClass inner = scan(Target.Inner.class);

        // assert
        assertThat(inner.typeArguments()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "[Lcom.acme.Order;,Order[]", "com.acme.Outer$Inner,Inner", "com.acme.Order,Order", "Main,Main" })
    void namesTypesLikeClassGetName(String name, String simpleName) {
        // act
        String result = DependencyAnalysis.simpleName(name);

        // assert
        assertThat(result).isEqualTo(simpleName);
    }

    @ParameterizedTest
    @CsvSource({ "java.lang.String,true", "javax.inject.Inject,true", "jakarta.inject.Inject,false",
            "javaxx.Foo,false" })
    void recognizesJdkTypes(String name, boolean core) {
        // act
        boolean result = TypeNames.isCoreType(name);

        // assert
        assertThat(result).isEqualTo(core);
    }
}
