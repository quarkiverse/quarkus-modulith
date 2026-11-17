package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.ReferenceMethodData;
import io.quarkiverse.modulith.core.data.TypeData;

class ValueTypesTest {

    private static final JavaType INNER = JavaType.of(new TypeData("a.b.Outer$Inner", Modifier.PUBLIC | Modifier.ABSTRACT
            | 0x0200 | 0x2000 | 0x4000, List.of("a.Base"), List.of("a.Iface"), List.of("a.Ann"), List.of("a.Meta"),
            List.of(), List.of(), List.of(), List.of()));
    private static final Violation FIRST = new Violation(Violation.Kind.CYCLE, "a", "a -> b", "", "first");
    private static final Violation SECOND = new Violation(Violation.Kind.FIELD_INJECTION, "b", "B.f", "C", "second");

    @ParameterizedTest
    @CsvSource({ "a.b,a.b,true", "a.b,a.b.c,true", "a.b,a.bc,false", "a.b.c,a.b,false" })
    void packageNamesContainTheirSubPackages(String name, String other, boolean contains) {
        // act
        boolean result = PackageName.of(name).contains(PackageName.of(other));

        // assert
        assertThat(result).isEqualTo(contains);
    }

    @ParameterizedTest
    @CsvSource({ "a.b,a.c,-1", "a.c,a.b,1", "a,a.b,-1", "a.b,a,1", "a.b,a.b,0" })
    void packageNamesCompareBySegment(String left, String right, int sign) {
        // act
        int result = PackageName.of(left).compareTo(PackageName.of(right));

        // assert
        assertThat(Integer.signum(result)).isEqualTo(sign);
    }

    @ParameterizedTest
    @CsvSource({ "a.b.api,api,true", "a.b.api,a*,true", "a.b.api,x,false", "a.b.api,a.b.*,true", "a.b.api,a.c.*,false",
            "a.b.api,?pi,true", "a.b.api,??,false", "a.bxapi,a.b.api,false", "a.b.api,a+,false", "a.b.a+,a+,true",
            "a.b.$x,$x,true", "a.b.x,[x],false" })
    void packageNamesMatchExpressionsWithOnlyTheWildcardsSpecial(String name, String expression, boolean matches) {
        // act
        boolean result = PackageName.of(name).nameContainsOrMatches(expression);

        // assert
        assertThat(result).isEqualTo(matches);
    }

    @ParameterizedTest
    @CsvSource({ "a.b.C,a.b", "Main,<<default>>", "a.b.Outer$Inner,a.b" })
    void packageNamesOfTypes(String type, String packageName) {
        // act
        PackageName name = PackageName.ofType(type);

        // assert
        assertThat(name).hasToString(packageName);
    }

    @ParameterizedTest
    @CsvSource({ "a.b,b", "a,a" })
    void packageNamesHaveALocalName(String name, String localName) {
        // act
        String result = PackageName.of(name).getLocalName();

        // assert
        assertThat(result).isEqualTo(localName);
    }

    @Test
    void packageNamesHaveAParent() {
        // act
        PackageName parent = PackageName.of("a.b").getParent();

        // assert
        assertThat(parent).isEqualTo(PackageName.of("a"));
    }

    @ParameterizedTest
    @CsvSource({ "a.b,a.b.c,true", "a.b,a.b.c.d,false", "a,a,false", "a.b,a.bc,false" })
    void packageNamesKnowTheirDirectChildren(String name, String other, boolean parent) {
        // act
        boolean result = PackageName.of(name).isDirectParentOf(PackageName.of(other));

        // assert
        assertThat(result).isEqualTo(parent);
    }

    @ParameterizedTest
    @CsvSource({ "Main,true", "a.Main,false" })
    void packageNamesRecognizeTheDefaultPackage(String type, boolean defaultPackage) {
        // act
        boolean result = PackageName.isDefault(PackageName.ofType(type));

        // assert
        assertThat(result).isEqualTo(defaultPackage);
    }

    @Test
    void packageNamesOfClasses() {
        // act
        PackageName name = PackageName.ofType(ValueTypesTest.class);

        // assert
        assertThat(name).isEqualTo(PackageName.of("io.quarkiverse.modulith.core"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "a;a.b.c;a.b,a.b.c", "a;a.b;a.b", "a;x;", "a;a;" })
    void packageNamesExpandUntilASubPackage(String name, String reference, String expanded) {
        // act
        List<String> result = PackageName.of(name).expandUntil(PackageName.of(reference)).map(PackageName::getName)
                .toList();

        // assert
        assertThat(result).isEqualTo(expanded == null ? List.of() : List.of(expanded.split(",")));
    }

    @Test
    void packageNamesAreEqualByName() {
        // act
        PackageName name = PackageName.of("a.b");

        // assert
        assertThat(name).isEqualTo(PackageName.of("a.b")).hasSameHashCodeAs(PackageName.of("a.b"))
                .isNotEqualTo(PackageName.of("a.c")).isNotEqualTo("a.b");
    }

    @ParameterizedTest
    @CsvSource({ "a.b.Type,a,b.Type", "a.b.c,a.b,c" })
    void packagesNameTheirSubPackagesRelatively(String sub, String base, String trailing) {
        // arrange
        JavaPackage basePackage = JavaPackage.of(List.of(), base);

        // act
        String result = basePackage.getTrailingName(JavaPackage.of(List.of(), sub));

        // assert
        assertThat(result).isEqualTo(trailing);
    }

    @ParameterizedTest
    @CsvSource({ "a.b,a.bc", "a.b,a.b", "a.b.c,a.b", "a.b,x" })
    void packagesRejectTrailingNamesOfOtherPackages(String base, String other) {
        // arrange
        JavaPackage basePackage = JavaPackage.of(List.of(), base);
        JavaPackage otherPackage = JavaPackage.of(List.of(), other);

        // act & assert
        assertThatThrownBy(() -> basePackage.getTrailingName(otherPackage)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Package " + other + " is not a sub-package of " + base);
    }

    @ParameterizedTest
    @CsvSource({ "isPublic,true", "isAbstract,true", "isInterface,true", "isAnnotation,true", "isEnum,true",
            "isPackageInfo,false" })
    void javaTypesDescribeTheirModifiers(String flag, boolean expected) {
        // act
        boolean result = flag(INNER, flag);

        // assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "isPublic", "isAbstract", "isInterface", "isAnnotation", "isEnum", "isPackageInfo" })
    void javaTypesWithoutModifiersHaveNoFlag(String flag) {
        // act
        boolean result = flag(Types.hiddenType("a.B"), flag);

        // assert
        assertThat(result).isFalse();
    }

    private static boolean flag(JavaType type, String flag) {
        Predicate<JavaType> predicate = switch (flag) {
            case "isPublic" -> JavaType::isPublic;
            case "isAbstract" -> JavaType::isAbstract;
            case "isInterface" -> JavaType::isInterface;
            case "isAnnotation" -> JavaType::isAnnotation;
            case "isEnum" -> JavaType::isEnum;
            default -> JavaType::isPackageInfo;
        };
        return predicate.test(type);
    }

    @Test
    void javaTypesHaveNames() {
        // act
        List<String> names = List.of(INNER.getSimpleName(), INNER.getFullName(), INNER.getPackageName());

        // assert
        assertThat(names).containsExactly("Inner", "a.b.Outer$Inner", "a.b");
    }

    @Test
    void javaTypesKeepTheirModifiers() {
        // act
        int modifiers = INNER.getModifiers();

        // assert
        assertThat(modifiers).isEqualTo(0x6601);
    }

    @ParameterizedTest
    @CsvSource({ "a.Ann,true", "a.Meta,false", "java.lang.Deprecated,false" })
    void javaTypesKnowTheirAnnotations(String annotation, boolean annotated) {
        // act
        boolean result = INNER.isAnnotatedWith(annotation);

        // assert
        assertThat(result).isEqualTo(annotated);
    }

    @ParameterizedTest
    @CsvSource({ "a.Ann,true", "a.Meta,true", "java.lang.Deprecated,false" })
    void javaTypesKnowTheirMetaAnnotations(String annotation, boolean annotated) {
        // act
        boolean result = INNER.isMetaAnnotatedWith(annotation);

        // assert
        assertThat(result).isEqualTo(annotated);
    }

    @Test
    void javaTypesMatchAnnotationClasses() {
        // act
        List<Boolean> annotated = List.of(INNER.isAnnotatedWith(Deprecated.class),
                INNER.isMetaAnnotatedWith(Deprecated.class));

        // assert
        assertThat(annotated).containsExactly(false, false);
    }

    @ParameterizedTest
    @CsvSource({ "a.Base,true", "a.b.Outer$Inner,true", "java.lang.Object,false" })
    void javaTypesKnowTheirSupertypes(String supertype, boolean assignable) {
        // act
        boolean result = INNER.isAssignableTo(supertype);

        // assert
        assertThat(result).isEqualTo(assignable);
    }

    @Test
    void javaTypesMatchSupertypeClasses() {
        // act
        boolean assignable = INNER.isAssignableTo(Object.class);

        // assert
        assertThat(assignable).isFalse();
    }

    @Test
    void javaTypesListTheirTypeNames() {
        // act
        List<List<String>> names = List.of(INNER.getInterfaceNames(), INNER.getSupertypeNames(),
                INNER.getAnnotationNames(), INNER.getMetaAnnotationNames());

        // assert
        assertThat(names).containsExactly(List.of("a.Iface"), List.of("a.Base"), List.of("a.Ann"), List.of("a.Meta"));
    }

    @Test
    void javaTypesAreEqualByName() {
        // act
        JavaType other = Types.hiddenType("a.b.Outer$Inner");

        // assert
        assertThat(INNER).hasToString("a.b.Outer$Inner").isEqualTo(other).hasSameHashCodeAs(other)
                .isNotEqualTo("a.b.Outer$Inner");
    }

    @Test
    void javaTypesCompareByName() {
        // act
        int result = INNER.compareTo(Types.hiddenType("a.c.A"));

        // assert
        assertThat(result).isNegative();
    }

    @Test
    void javaTypesLoadTheirClass() {
        // arrange
        JavaType type = Types.publicType(ValueTypesTest.class.getName());

        // act
        Class<?> loaded = type.reflect();

        // assert
        assertThat(loaded).isEqualTo(ValueTypesTest.class);
    }

    @Test
    void javaTypesReportClassesThatCannotBeLoaded() {
        // arrange
        JavaType type = Types.publicType("a.Missing");

        // act & assert
        assertThatThrownBy(type::reflect).isInstanceOf(IllegalStateException.class).hasMessage("Cannot load a.Missing");
    }

    @ParameterizedTest
    @CsvSource({ "com.acme.order.Order,com.acme,c.a.order.Order", "com.acme.Order,com.acme,c.a.Order",
            "org.other.Thing,com.acme,org.other.Thing", "Main,com.acme,Main",
            "com.acme.order.Order$Line,com.acme,c.a.order.Order.Line",
            "com.acme.orders.Thing,com.acme.order,com.acme.orders.Thing",
            "com.acmeorders.Thing,com.acme,com.acmeorders.Thing" })
    void formattableTypesAbbreviateTheModulePackage(String type, String module, String abbreviated) {
        // arrange
        ApplicationModule applicationModule = Modules.module("m", module);

        // act
        String result = FormattableType.of(type).getAbbreviatedFullName(applicationModule);

        // assert
        assertThat(result).isEqualTo(abbreviated);
    }

    @ParameterizedTest
    @CsvSource({ "a.b.c.D,a.b.c.D", "a.b.c.D$E,a.b.c.D.E", "D,D" })
    void formattableTypesHaveAFullName(String type, String fullName) {
        // act
        String result = FormattableType.of(type).getFullName();

        // assert
        assertThat(result).isEqualTo(fullName);
    }

    @ParameterizedTest
    @CsvSource({ "x.y.Type,x.y.Type", "com.acme.Type,c.a.Type", "Type,Type", "com.acme.Outer$Inner,c.a.Outer.Inner" })
    void formattableTypesAbbreviateTheirPackage(String type, String abbreviated) {
        // act
        String result = FormattableType.of(type).getAbbreviatedFullName();

        // assert
        assertThat(result).isEqualTo(abbreviated);
    }

    @Test
    void formattableTypesOfClasses() {
        // act
        List<String> names = List.of(FormattableType.of(String.class).getAbbreviatedFullName(),
                FormattableType.of(String[].class).getFullName());

        // assert
        assertThat(names).containsExactly("j.l.String", "java.lang.String[]");
    }

    @Test
    void formattableTypesOfJavaTypes() {
        // act
        FormattableType type = FormattableType.of(Types.publicType("a.b.C"));

        // assert
        assertThat(type).hasToString("a.b.C");
    }

    @Test
    void formattableTypesAbbreviateWithoutModule() {
        // act
        String result = FormattableType.of("com.acme.Type").getAbbreviatedFullName(null);

        // assert
        assertThat(result).isEqualTo("c.a.Type");
    }

    @Test
    void formattableTypesAbbreviateForAModuleInTheDefaultPackage() {
        // act
        String result = FormattableType.of("com.acme.Type").getAbbreviatedFullName(Modules.module("m", ""));

        // assert
        assertThat(result).isEqualTo("c.a.Type");
    }

    @Test
    void formattableTypesAbbreviateTypesOutsideTheModuleOnRequest() {
        // act
        String result = FormattableType.of("com.acme.Type").getAbbreviatedFullName(Modules.module("m", "a.b"),
                NonModuleTypeAbbreviation.ABBREVIATED);

        // assert
        assertThat(result).isEqualTo("c.a.Type");
    }

    @Test
    void formattableTypesFormatLists() {
        // act
        String result = FormattableType.format(List.of(Types.publicType("com.acme.C"), Types.publicType("D")));

        // assert
        assertThat(result).isEqualTo("c.a.C, D");
    }

    @ParameterizedTest
    @CsvSource({ "USES_COMPONENT,Component a.B using c.D", "ENTITY,Entity a.B depending on c.D",
            "EVENT_LISTENER,a.B listening to events of type c.D", "DEFAULT,a.B depending on c.D" })
    void dependencyTypesFormatDependencies(DependencyType type, String expected) {
        // act
        String formatted = type.format(FormattableType.of("a.B"), FormattableType.of("c.D"));

        // assert
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void dependencyTypesExcludeOthers() {
        // act
        Set<DependencyType> types = DependencyType.allBut(DependencyType.DEFAULT, DependencyType.ENTITY);

        // assert
        assertThat(types).containsExactly(DependencyType.USES_COMPONENT, DependencyType.EVENT_LISTENER);
    }

    @Test
    void dependencyTypesExcludeNothingByDefault() {
        // act
        Set<DependencyType> types = DependencyType.allBut();

        // assert
        assertThat(types).containsExactlyInAnyOrder(DependencyType.values());
    }

    @Test
    void identifiersHaveAValue() {
        // act
        ApplicationModuleIdentifier identifier = ApplicationModuleIdentifier.of("a");

        // assert
        assertThat(identifier).hasToString("a");
    }

    @Test
    void identifiersCompareByValue() {
        // act
        int result = ApplicationModuleIdentifier.of("a").compareTo(ApplicationModuleIdentifier.of("b"));

        // assert
        assertThat(result).isNegative();
    }

    @ParameterizedTest
    @CsvSource(value = { "' '", "''", "NULL" }, nullValues = "NULL")
    void identifiersMustNotBeBlank(String value) {
        // act & assert
        assertThatThrownBy(() -> ApplicationModuleIdentifier.of(value)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Module identifier must not be blank");
    }

    @Test
    void violationsCombineByMessage() {
        // act
        Violations violations = Violations.NONE.and(FIRST).and(SECOND).and(FIRST);

        // assert
        assertThat(violations.getMessages()).containsExactly("first", "second");
    }

    @Test
    void violationsDescribeTheirMessages() {
        // act
        Violations violations = Violations.NONE.and(FIRST).and(SECOND);

        // assert
        assertThat(violations.getMessage()).isEqualTo("- first\n- second");
    }

    @Test
    void noViolationsHaveAMessageOfTheirOwn() {
        // act
        String message = Violations.NONE.getMessage();

        // assert
        assertThat(message).isEqualTo("No module violations");
    }

    @Test
    void violationsStayTheSameWithoutNewMessages() {
        // arrange
        Violations violations = new Violations(List.of(FIRST));

        // act
        Violations combined = violations.and(Violations.NONE).and(FIRST);

        // assert
        assertThat(combined).isSameAs(violations);
    }

    @Test
    void violationsFilterByPredicateAndKind() {
        // arrange
        Violations violations = new Violations(List.of(FIRST, SECOND));

        // act
        Violations cycles = violations.filter(it -> it.kind() == Violation.Kind.CYCLE);

        // assert
        assertThat(cycles.getViolations()).containsExactly(FIRST);
        assertThat(violations.ofKind(Violation.Kind.FIELD_INJECTION)).containsExactly(SECOND);
    }

    @ParameterizedTest
    @CsvSource({ "irs,true", "x,false" })
    void violationsSearchTheirMessage(String text, boolean contained) {
        // act
        boolean result = FIRST.hasMessageContaining(text);

        // assert
        assertThat(result).isEqualTo(contained);
    }

    @Test
    void violationsAreTheirMessage() {
        // act
        String message = FIRST.getMessage();

        // assert
        assertThat(message).isEqualTo("first");
        assertThat(FIRST).hasToString("first");
    }

    @Test
    void noViolationsThrowNothing() {
        // act & assert
        assertThatCode(Violations.NONE::throwIfPresent).doesNotThrowAnyException();
    }

    @Test
    void violationsThrowThemselves() {
        // arrange
        Violations violations = new Violations(List.of(FIRST));

        // act & assert
        assertThatThrownBy(violations::throwIfPresent).isSameAs(violations);
    }

    @Test
    void violationsSurviveSerialization() throws IOException, ClassNotFoundException {
        // arrange
        Violations violations = new Violations(List.of(FIRST, SECOND));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(violations);
        }

        // act
        Violations restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (Violations) in.readObject();
        }

        // assert
        assertThat(restored.getViolations()).containsExactly(FIRST, SECOND);
        assertThat(restored.hasViolations()).isTrue();
        assertThat(restored.getMessage()).isEqualTo("- first\n- second");
    }

    @Test
    void referenceMethodsDescribeListeners() {
        // arrange
        JavaType owner = Types.publicType("a.Listener");

        // act
        ReferenceMethod method = new ReferenceMethod(owner,
                new ReferenceMethodData("on", List.of("a.Event"), true, "AFTER_COMMIT", List.of("a.Event")));

        // assert
        assertThat(method.getOwner()).isEqualTo(owner);
        assertThat(method.getName()).isEqualTo("on");
        assertThat(method.getParameterTypeNames()).containsExactly("a.Event");
        assertThat(method.isAsync()).isTrue();
        assertThat(method.getTransactionPhase()).hasValue("AFTER_COMMIT");
        assertThat(method.getReferenceTypeNames()).containsExactly("a.Event");
        assertThat(method).hasToString("a.Listener.on(a.Event)");
    }

    @Test
    void referenceMethodsWithoutPhaseHaveNone() {
        // arrange
        JavaType owner = Types.publicType("a.Listener");

        // act
        ReferenceMethod method = new ReferenceMethod(owner, new ReferenceMethodData("on", List.of(), false, "",
                List.of()));

        // assert
        assertThat(method.getTransactionPhase()).isEmpty();
    }

    @Test
    void annotationDataReadsAttributes() {
        // arrange
        AnnotationData annotation = Types.namedInterface("a", "b");

        // act
        List<String> values = annotation.values("value").orElseThrow();

        // assert
        assertThat(values).containsExactly("a", "b");
        assertThat(annotation.value("value")).hasValue("a");
        assertThat(annotation.value("name")).isEmpty();
    }

    @Test
    void annotationDataWithoutAttributesHasNoValues() {
        // act
        AnnotationData annotation = Types.namedInterface();

        // assert
        assertThat(annotation.values("value")).isEmpty();
    }
}
