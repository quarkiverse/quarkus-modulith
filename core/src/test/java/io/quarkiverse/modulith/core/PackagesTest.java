package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.AttributeData;
import io.quarkiverse.modulith.core.data.TypeData;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;

class PackagesTest {

    static final List<JavaType> TYPES = List.of(Types.publicType("a.A"), Types.hiddenType("a.Hidden"),
            Types.packageInfo("a"), Types.publicType("a.b.B"), Types.publicType("a.b.c.C"), Types.publicType("a.d.D"),
            Types.packageInfo("a.d", Types.namedInterface("dee")), Types.publicType("x.X"));
    static final JavaPackage A = JavaPackage.of(TYPES, "a");
    static final String APPLICATION_MODULE = "io.quarkiverse.modulith.ApplicationModule";
    static final String JMOLECULES_MODULE = "org.jmolecules.ddd.annotation.Module";

    static JavaPackage declaring(String annotation, AttributeData... attributes) {
        return JavaPackage.of(List.of(Types.packageInfo("m", new AnnotationData(annotation, List.of(attributes)))), "m");
    }

    @Test
    void packagesHoldTheTypesOfTheirSubPackages() {
        // act
        List<String> types = A.stream().map(JavaType::getName).toList();

        // assert
        assertThat(types).containsExactly("a.A", "a.Hidden", "a.b.B", "a.b.c.C", "a.d.D", "a.d.package-info",
                "a.package-info");
        assertThat((Iterable<JavaType>) A).extracting(JavaType::getName).containsExactlyElementsOf(types);
    }

    @Test
    void singlePackagesHoldOnlyTheirOwnTypes() {
        // act
        JavaPackage single = A.toSingle();

        // assert
        assertThat(single.getTypes()).extracting(JavaType::getName).containsExactly("a.A", "a.Hidden", "a.package-info");
        assertThat(single.getSubPackages()).isEmpty();
    }

    @Test
    void packagesExposeTheirPublicTypes() {
        // act
        List<JavaType> exposed = A.getExposedTypes();

        // assert
        assertThat(exposed).extracting(JavaType::getName).containsExactly("a.A", "a.b.B", "a.b.c.C", "a.d.D");
    }

    @Test
    void packagesDoNotExposePackageInfoTypes() {
        // arrange
        JavaPackage pkg = JavaPackage.of(List.of(JavaType.of(new TypeData("q.package-info", Modifier.PUBLIC, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))), "q");

        // act
        List<JavaType> exposed = pkg.getExposedTypes();

        // assert
        assertThat(exposed).isEmpty();
    }

    @Test
    void packagesListTheirSubPackages() {
        // act
        JavaPackages subPackages = A.getSubPackages();

        // assert
        assertThat(subPackages).extracting(JavaPackage::getName).containsExactly("a.b", "a.b.c", "a.d");
        assertThat(A.getDirectSubPackages()).extracting(JavaPackage::getName).containsExactly("a.b", "a.d");
        assertThat(A.getSubPackagesMatching((it, trailing) -> trailing.contains("."))).extracting(JavaPackage::getName)
                .containsExactly("a.b.c");
    }

    @Test
    void packagesHaveNames() {
        // act
        String trailing = A.getTrailingName(JavaPackage.of(TYPES, "a.b.c"));

        // assert
        assertThat(trailing).isEqualTo("b.c");
        assertThat(A.getLocalName()).isEqualTo("a");
        assertThat(A.getPackageName()).isEqualTo(PackageName.of("a"));
    }

    @Test
    void packagesAreEqualByNameAndTypes() {
        // act
        JavaPackage same = JavaPackage.of(TYPES, "a");

        // assert
        assertThat((Object) A).hasToString("a").isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(A.toSingle())
                .isNotEqualTo(JavaPackage.of(TYPES, "x")).isNotEqualTo("a");
        assertThat(A.compareTo(JavaPackage.of(TYPES, "x"))).isNegative();
    }

    @ParameterizedTest
    @CsvSource({ "a.b.B,true", "x.X,false", "a.Missing,false" })
    void packagesKnowTheirTypesByName(String type, boolean contained) {
        // act
        boolean result = A.contains(type);

        // assert
        assertThat(result).isEqualTo(contained);
    }

    @Test
    void packagesKnowTheirTypesAndClasses() {
        // act
        boolean containsType = A.contains(Types.publicType("a.A"));

        // assert
        assertThat(containsType).isTrue();
        assertThat(A.contains(String.class)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "a.z.Z,true", "a.Q,true", "x.Z,false", "java.lang.String,false" })
    void packagesCouldContainTypesOfTheirPackages(String type, boolean possible) {
        // act
        boolean result = A.couldContain(type);

        // assert
        assertThat(result).isEqualTo(possible);
        assertThat(new JavaPackages(List.of(A)).couldContain(type)).isEqualTo(possible);
    }

    @Test
    void packagesCouldNotContainJdkClasses() {
        // act
        boolean possible = A.couldContain(String.class);

        // assert
        assertThat(possible).isFalse();
        assertThat(new JavaPackages(List.of(A)).couldContain(String.class)).isFalse();
    }

    @Test
    void packagesSelectTypes() {
        // act
        List<JavaType> packageInfos = A.that(JavaType::isPackageInfo);

        // assert
        assertThat(packageInfos).extracting(JavaType::getName).containsExactly("a.d.package-info", "a.package-info");
    }

    @Test
    void packageCollectionsHoldPackages() {
        // act
        JavaPackages packages = new JavaPackages(List.of(A));

        // assert
        assertThat(packages.contains(A)).isTrue();
        assertThat(packages).hasToString("[a]").extracting(JavaPackage::getName).containsExactly("a");
    }

    @ParameterizedTest
    @CsvSource({ "a.package-info,true", "a.A,false" })
    void recognizesPackageInfoTypes(String type, boolean packageInfo) {
        // arrange
        JavaType candidate = type.endsWith("package-info") ? Types.packageInfo("a") : Types.publicType(type);

        // act
        boolean result = JavaPackage.isPackageInfoType(candidate);

        // assert
        assertThat(result).isEqualTo(packageInfo);
    }

    @Test
    void recognizesPackageInfoMarkers() {
        // act
        boolean result = JavaPackage.isPackageInfoType(Types.marker("a.Marker"));

        // assert
        assertThat(result).isTrue();
    }

    @Test
    void packagesFindTheirAnnotatedSubPackages() {
        // act
        List<String> annotated = A.getSubPackagesAnnotatedWith(io.quarkiverse.modulith.NamedInterface.class)
                .map(JavaPackage::getName).toList();

        // assert
        assertThat(annotated).containsExactly("a.d");
    }

    @ParameterizedTest
    @CsvSource({ "a.d,true", "a,false" })
    void packagesKnowTheirAnnotations(String name, boolean annotated) {
        // act
        boolean result = JavaPackage.of(TYPES, name).isAnnotatedWith(io.quarkiverse.modulith.NamedInterface.class);

        // assert
        assertThat(result).isEqualTo(annotated);
    }

    @Test
    void packagesRejectTwoDeclarations() {
        // arrange
        JavaPackage pkg = JavaPackage.of(List.of(Types.packageInfo("q", Types.namedInterface()),
                Types.marker("q.Marker", Types.namedInterface()), Types.publicType("q.Other")), "q");

        // act & assert
        assertThatThrownBy(() -> pkg.findDeclaration(NamedInterface.ANNOTATIONS))
                .isInstanceOf(IllegalStateException.class).hasMessage(
                        "Expected maximum of one type in package q to be annotated with io.quarkiverse.modulith.NamedInterface"
                                + " or org.springframework.modulith.NamedInterface, but got [q.Marker, q.package-info]!");
    }

    @Test
    void packagesLoadTheirAnnotations() {
        // arrange
        JavaPackage pkg = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS).getModuleByName("kernel")
                .orElseThrow().getJavaPackage();

        // act
        Optional<io.quarkiverse.modulith.ApplicationModule> annotation = pkg
                .getAnnotation(io.quarkiverse.modulith.ApplicationModule.class);

        // assert
        assertThat(annotation.map(io.quarkiverse.modulith.ApplicationModule::displayName)).hasValue("Shared kernel");
        assertThat(pkg.getAnnotation(Deprecated.class)).isEmpty();
    }

    @Test
    void namedInterfacesMergeByName() {
        // arrange
        NamedInterface first = NamedInterface.of("n", List.of(Types.publicType("a.A")), List.of("a"));
        NamedInterface second = NamedInterface.of("n", List.of(Types.publicType("a.B")), List.of("a", "b"));

        // act
        NamedInterfaces merged = NamedInterfaces.of(List.of(first)).and(List.of(second, NamedInterface.of("o", List.of())));

        // assert
        assertThat(merged.stream()).extracting(NamedInterface::getName).containsExactly("n", "o");
        assertThat(merged.getByName("n").map(NamedInterface::getPackages)).hasValue(List.of("a", "b"));
        assertThat(merged.getByName("n").orElseThrow()).extracting(JavaType::getName).containsExactly("a.A", "a.B");
    }

    @Test
    void namedInterfacesMergeTheGivenOnesWithTheSameName() {
        // arrange
        NamedInterface first = NamedInterface.of("spi", List.of(Types.publicType("a.spi.A")), List.of("a.spi"));
        NamedInterface second = NamedInterface.of("spi", List.of(Types.publicType("b.spi.B")), List.of("b.spi"));

        // act
        NamedInterfaces merged = NamedInterfaces.NONE.and(List.of(first, second));

        // assert
        assertThat(merged.stream()).singleElement().satisfies(it -> {
            assertThat(it.getPackages()).containsExactly("a.spi", "b.spi");
            assertThat(it).extracting(JavaType::getName).containsExactly("a.spi.A", "b.spi.B");
        });
    }

    @Test
    void namedInterfacesFindTheInterfacesOfTypes() {
        // arrange
        NamedInterfaces interfaces = NamedInterfaces.of(List.of(NamedInterface.of("n", List.of(Types.publicType("a.A"),
                Types.publicType("a.B")))));

        // act
        boolean containsA = interfaces.containsInExplicitInterface(Types.publicType("a.A"));

        // assert
        assertThat(containsA).isTrue();
        assertThat(interfaces.containsInExplicitInterface("a.C")).isFalse();
        assertThat(interfaces.getNamedInterfacesContaining(Types.publicType("a.B"))).extracting(NamedInterface::getName)
                .containsExactly("n");
    }

    @Test
    void namedInterfacesKnowWhetherTheyAreExplicit() {
        // act
        NamedInterfaces named = NamedInterfaces.of(List.of(NamedInterface.of("n", List.of())));

        // assert
        assertThat(named.hasExplicitInterfaces()).isTrue();
        assertThat(NamedInterfaces.NONE.hasExplicitInterfaces()).isFalse();
        assertThat(NamedInterfaces.of(List.of())).isSameAs(NamedInterfaces.NONE);
    }

    @Test
    void namedInterfacesDescribeThemselves() {
        // act
        NamedInterfaces interfaces = NamedInterfaces.of(List.of(NamedInterface.of("n", List.of(Types.publicType("a.A"),
                Types.publicType("a.B"))), NamedInterface.of("o", List.of())));

        // assert
        assertThat(interfaces).hasToString("NamedInterface: name=n, types=[ a.A, a.B ]" + System.lineSeparator()
                + "NamedInterface: name=o, types=[]");
    }

    @Test
    void namedInterfacesRequireAnUnnamedOneOnRequest() {
        // arrange
        NamedInterfaces interfaces = NamedInterfaces.of(List.of(NamedInterface.of("n", List.of())));

        // act & assert
        assertThatThrownBy(interfaces::getUnnamedInterface).isInstanceOf(IllegalStateException.class)
                .hasMessage("No unnamed interface found!");
    }

    @Test
    void namedInterfacesAreEqualByNameAndTypes() {
        // act
        NamedInterface first = NamedInterface.of("n", List.of(Types.publicType("a.A")), List.of("a"));

        // assert
        assertThat(first).isEqualTo(NamedInterface.of("n", List.of(Types.publicType("a.A"))))
                .hasSameHashCodeAs(NamedInterface.of("n", List.of(Types.publicType("a.A"))))
                .isNotEqualTo(NamedInterface.of("n", List.of(Types.publicType("a.B"))))
                .isNotEqualTo(NamedInterface.of("m", List.of(Types.publicType("a.A")))).isNotEqualTo("n");
    }

    @Test
    void namedInterfacesHoldTheirTypes() {
        // act
        NamedInterface first = NamedInterface.of("n", List.of(Types.publicType("a.A")), List.of("a"));

        // assert
        assertThat(first).extracting(JavaType::getName).containsExactly("a.A");
        assertThat(first.contains(String.class)).isFalse();
        assertThat(first.contains("a.A")).isTrue();
        assertThat(first.isNamed()).isTrue();
    }

    @Test
    void discoversTheUnnamedInterface() {
        // act
        NamedInterfaces interfaces = NamedInterfaces.discoverNamedInterfaces(A);

        // assert
        assertThat(interfaces.getUnnamedInterface()).extracting(JavaType::getName).containsExactly("a.A");
    }

    @Test
    void discoversTheInterfacesOfAnnotatedPackages() {
        // act
        NamedInterfaces interfaces = NamedInterfaces.discoverNamedInterfaces(A);

        // assert
        assertThat(interfaces.getByName("dee").orElseThrow()).extracting(JavaType::getName).containsExactly("a.d.D");
    }

    @Test
    void exposesThePublicTypesOfOpenModules() {
        // act
        NamedInterfaces interfaces = NamedInterfaces.forOpen(A);

        // assert
        assertThat(interfaces.getUnnamedInterface()).extracting(JavaType::getName).containsExactly("a.A", "a.b.B",
                "a.b.c.C");
    }

    @Test
    void rejectsNamedInterfacesOfPackagesWithoutAnnotation() {
        // arrange
        JavaPackage pkg = JavaPackage.of(TYPES, "a.b");

        // act & assert
        assertThatThrownBy(() -> NamedInterface.of(pkg)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Couldn't find NamedInterface annotation on package a.b!");
    }

    @Test
    void rejectsDefaultedNamedInterfacesInTheBasePackage() {
        // arrange
        JavaPackage pkg = JavaPackage.of(List.of(Types.publicType("q.Q", Types.namedInterface())), "q");

        // act & assert
        assertThatThrownBy(() -> NamedInterface.unnamed(pkg)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot use named interface defaulting for type(s) q.Q located in base package!");
    }

    @ParameterizedTest
    @CsvSource({ "value", "name" })
    void leavesNamedTypesOutOfTheUnnamedInterface(String attribute) {
        // arrange
        JavaPackage pkg = JavaPackage.of(List.of(Types.publicType("q.Q", new AnnotationData(
                "io.quarkiverse.modulith.NamedInterface", List.of(new AttributeData(attribute, List.of("x")))))), "q");

        // act
        NamedInterface unnamed = NamedInterface.unnamed(pkg);

        // assert
        assertThat(unnamed).isEmpty();
    }

    @Test
    void relatesNoTypesToHiddenTypes() {
        // act
        Set<JavaType> related = NamedInterfaces.relatedTypesOf(Types.hiddenType("a.Hidden"), TYPES);

        // assert
        assertThat(related).isEmpty();
    }

    @Test
    void buildsTheUnnamedInterfaceInCode() {
        // arrange
        NamedInterfacesBuilder builder = NamedInterfaces.builder(A);

        // act
        NamedInterfaces direct = builder.build();

        // assert
        assertThat(direct.stream()).extracting(NamedInterface::getName).containsExactly(NamedInterface.UNNAMED_NAME);
    }

    @Test
    void buildsInterfacesOfMatchingSubPackages() {
        // arrange
        NamedInterfacesBuilder builder = NamedInterfaces.builder(A);

        // act
        NamedInterfaces matching = builder.matching("b", "d").build();

        // assert
        assertThat(matching.stream()).extracting(NamedInterface::getName).containsExactly(NamedInterface.UNNAMED_NAME,
                "b", "d");
    }

    @Test
    void buildsInterfacesOfNestedSubPackages() {
        // arrange
        NamedInterfacesBuilder builder = NamedInterfaces.builder(A).recursive();

        // act
        NamedInterfaces matching = builder.matching(List.of("b.c")).build();

        // assert
        assertThat(matching.getByName("b.c").orElseThrow()).extracting(JavaType::getName).containsExactly("a.b.c.C");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "true;c;b,d", "false;d;b" })
    void buildsInterfacesWithoutExcludedSubPackages(boolean recursive, String excluded, String names) {
        // arrange
        NamedInterfacesBuilder builder = recursive ? NamedInterfaces.builder(A).recursive() : NamedInterfaces.builder(A);

        // act
        NamedInterfaces interfaces = builder.including(it -> true).excluding(List.of(excluded)).build();

        // assert
        assertThat(interfaces.stream()).extracting(NamedInterface::getName).containsExactlyElementsOf(
                Stream.concat(Stream.of(NamedInterface.UNNAMED_NAME), Stream.of(names.split(","))).toList());
    }

    @Test
    void buildsInterfacesWithoutExcludedExpressions() {
        // arrange
        NamedInterfacesBuilder builder = NamedInterfaces.builder(A).including(it -> true);

        // act
        NamedInterfaces interfaces = builder.excluding("b", "x?").build();

        // assert
        assertThat(interfaces.stream()).extracting(NamedInterface::getName).containsExactly(NamedInterface.UNNAMED_NAME,
                "d");
    }

    @Test
    void buildsInterfacesWithoutPackagesOfAPredicate() {
        // arrange
        NamedInterfacesBuilder builder = NamedInterfaces.builder(A).including(it -> true);

        // act
        NamedInterfaces interfaces = builder.excluding(it -> it.getLocalName().equals("b")).build();

        // assert
        assertThat(interfaces.getByName("b").map(NamedInterface::getPackages)).hasValue(List.of("a.b"));
    }

    @Test
    void readsTheJMoleculesModuleDeclaration() {
        // arrange
        JavaPackage jMolecules = declaring(JMOLECULES_MODULE, new AttributeData("value", List.of("Named")));

        // act
        ApplicationModuleInformation information = ApplicationModuleInformation.of(jMolecules);

        // assert
        assertThat(information.getDisplayName()).hasValue("Named");
        assertThat(information.getDeclaredDependencies()).containsExactly(ApplicationModuleInformation.OPEN_TOKEN);
        assertThat(information.isOpen()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ JMOLECULES_MODULE + ",value", APPLICATION_MODULE + ",displayName" })
    void ignoresBlankDisplayNames(String annotation, String attribute) {
        // arrange
        JavaPackage pkg = declaring(annotation, new AttributeData(attribute, List.of(" ")),
                new AttributeData("id", List.of(" ")));

        // act
        Optional<String> displayName = ApplicationModuleInformation.of(pkg).getDisplayName();

        // assert
        assertThat(displayName).isEmpty();
    }

    @Test
    void ignoresBlankIdentifiers() {
        // arrange
        JavaPackage pkg = declaring(APPLICATION_MODULE, new AttributeData("id", List.of(" ")));

        // act
        Optional<String> identifier = AnnotationModuleInformation.of(pkg).getIdentifier();

        // assert
        assertThat(identifier).isEmpty();
    }

    @Test
    void declaresNoDisplayNameByDefault() {
        // act
        Optional<String> displayName = new OpenInformation().getDisplayName();

        // assert
        assertThat(displayName).isEmpty();
    }

    @Test
    void sourcesDescribeModules() {
        // arrange
        JavaPackage pkg = JavaPackage.of(TYPES, "a.b");

        // act
        ApplicationModuleSource source = ApplicationModuleSource.from(pkg, "b");

        // assert
        assertThat(source.getIdentifier()).isEqualTo(ApplicationModuleIdentifier.of("b"));
        assertThat((Object) source.getModuleBasePackage()).isEqualTo(pkg);
        assertThat(source.getNamedInterfaces(ApplicationModuleInformation.of(pkg), pkg).getUnnamedInterface())
                .extracting(JavaType::getName).containsExactly("a.b.B");
    }

    @Test
    void sourcesAreEqualByIdentifierAndPackage() {
        // arrange
        JavaPackage pkg = JavaPackage.of(TYPES, "a.b");

        // act
        ApplicationModuleSource source = ApplicationModuleSource.from(pkg, "b");

        // assert
        assertThat(source).hasToString("ApplicationModuleSource(b, a.b)").isEqualTo(ApplicationModuleSource.from(pkg, "b"))
                .hasSameHashCodeAs(ApplicationModuleSource.from(pkg, "b"))
                .isNotEqualTo(ApplicationModuleSource.from(pkg, "c"))
                .isNotEqualTo(ApplicationModuleSource.from(JavaPackage.of(TYPES, "a.d"), "b")).isNotEqualTo("b");
    }

    @Test
    void findsNoExplicitlyAnnotatedModulesWithoutAnnotations() {
        // act
        List<ApplicationModuleSource> sources = ApplicationModuleSource.from(A,
                ApplicationModuleDetectionStrategy.explicitlyAnnotated(), true).toList();

        // assert
        assertThat(sources).isEmpty();
    }

    @Test
    void sourceFactoriesContributeNothingByDefault() {
        // act
        ApplicationModuleSourceFactory factory = new ListedModulesFactory();

        // assert
        assertThat(factory.getRootPackages()).isEmpty();
        assertThat(factory.getApplicationModuleDetectionStrategy()).isNull();
    }

    @ParameterizedTest
    @CsvSource({ "true,a.b", "false,b" })
    void sourceFactoriesNameTheirModules(boolean fullyQualified, String identifier) {
        // arrange
        ApplicationModuleSourceFactory factory = new ListedModulesFactory();

        // act
        List<String> identifiers = factory.getApplicationModuleSources(it -> JavaPackage.of(TYPES, it), fullyQualified)
                .map(it -> it.getIdentifier().toString()).toList();

        // assert
        assertThat(identifiers).containsExactly(identifier);
    }

    @Test
    void sourceFactoriesDetectModulesWithTheStrategy() {
        // arrange
        ApplicationModuleSourceFactory factory = new ListedModulesFactory();

        // act
        List<String> identifiers = factory.getApplicationModuleSources(A,
                ApplicationModuleDetectionStrategy.directSubPackage(), false).map(it -> it.getIdentifier().toString())
                .toList();

        // assert
        assertThat(identifiers).containsExactly("b", "d");
    }
}
