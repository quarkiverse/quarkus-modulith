package io.quarkiverse.modulith.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class OptionsTest {

    static Stream<Arguments> invalidOptions() {
        return Stream.of(
                Arguments.of((Supplier<Object>) () -> DiagramOptions.defaults().withTargetFileName("diagram.puml"),
                        "Configured file name pattern 'diagram.puml' does not include a '%s' placeholder for the module name!"),
                Arguments.of((Supplier<Object>) () -> DiagramOptions.defaults().withSkinParam(" ", "x"),
                        "Name must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> DiagramOptions.defaults().withSkinParam(null, "x"),
                        "Name must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> CanvasOptions.defaults().withApiBase(" "),
                        "API base must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> CanvasOptions.defaults().withApiBase(null),
                        "API base must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> CanvasOptions.defaults().groupingBy("A", it -> true, " "),
                        "Description must not be null!"),
                Arguments.of((Supplier<Object>) () -> CanvasOptions.defaults().groupingBy("A", it -> true, null),
                        "Description must not be null!"),
                Arguments.of((Supplier<Object>) () -> Grouping.of(" ", it -> true),
                        "Name must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> Grouping.of(null, it -> true),
                        "Name must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> Grouping.of("A", it -> true, " "),
                        "Description must not be empty or null!"),
                Arguments.of((Supplier<Object>) () -> Grouping.nameMatching(" "),
                        "Pattern must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> Grouping.nameMatching(null),
                        "Pattern must not be null or empty!"),
                Arguments.of((Supplier<Object>) () -> DocumenterOptions.defaults().withOutputFolder(""),
                        "Output folder must not be empty!"),
                Arguments.of((Supplier<Object>) () -> DocumenterOptions.defaults().withOutputFolder(" "),
                        "Output folder must not be empty!"));
    }

    @ParameterizedTest
    @MethodSource("invalidOptions")
    void rejectsInvalidOptions(Supplier<Object> options, String message) {
        // act
        Throwable thrown = catchThrowable(options::get);

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    @Test
    void rejectsAnInvalidNamePatternWhenCreated() {
        // act
        Throwable thrown = catchThrowable(() -> Grouping.nameMatching("com.acme.[Service"));

        // assert
        assertThat(thrown).isInstanceOf(PatternSyntaxException.class)
                .hasMessageStartingWith("Unclosed character class near index 16");
    }

    @Test
    void groupingKeepsNameAndDescription() {
        // arrange
        ModuleBean bean = new ModuleBean(ModuleType.of("com.acme.Service"), true, List.of(), Set.of(), List.of());

        // act
        Grouping described = Grouping.of("Services", it -> true, "Do things");
        Grouping plain = Grouping.of("Others", it -> false, null);
        Throwable withoutBean = catchThrowable(() -> plain.matches(null));

        // assert
        assertThat(described.getName()).isEqualTo("Services");
        assertThat(described.getDescription()).contains("Do things");
        assertThat(described.matches(bean)).isTrue();
        assertThat(plain.getDescription()).isEmpty();
        assertThat(plain.matches(bean)).isFalse();
        assertThat(withoutBean).isInstanceOf(NullPointerException.class);
    }

    @Test
    void groupingPredicatesMatchByTypeName() {
        // arrange
        ModuleBean bean = new ModuleBean(new ModuleType("com.acme.Impl$Inner", true, true, Set.of("com.acme.Marker"),
                Set.of(), Set.of(), Set.of("com.acme.Api")), true, List.of(), Set.of(), List.of());
        ModuleBean api = new ModuleBean(ModuleType.of("com.acme.Api"), true, List.of(), Set.of(), List.of());

        // act
        boolean named = Grouping.nameMatching(".*\\$Inner").test(bean);
        boolean partial = Grouping.nameMatching("Inner").test(bean);
        boolean implementing = Grouping.implementing("com.acme.Api").test(bean);
        boolean implementingItself = Grouping.implementing("com.acme.Api").test(api);
        boolean subtype = Grouping.subtypeOf("com.acme.Api").test(bean);
        boolean subtypeOfItself = Grouping.subtypeOf("com.acme.Api").test(api);
        boolean annotated = Grouping.isAnnotatedWith("com.acme.Marker").test(bean);
        boolean byClass = Grouping.isAnnotatedWith(Deprecated.class).test(bean);
        boolean implementingObject = Grouping.implementing(Object.class).test(api);
        boolean subtypeOfObject = Grouping.subtypeOf(Object.class).test(api);

        // assert
        assertThat(named).isTrue();
        assertThat(partial).isFalse();
        assertThat(implementing).isTrue();
        assertThat(implementingItself).isTrue();
        assertThat(subtype).isTrue();
        assertThat(subtypeOfItself).isFalse();
        assertThat(annotated).isTrue();
        assertThat(byClass).isFalse();
        assertThat(implementingObject).isFalse();
        assertThat(subtypeOfObject).isFalse();
        assertThat(api.type().packageName()).isEqualTo("com.acme");
    }

    @Test
    void groupsBeansByFirstMatchingGroup() {
        // arrange
        ModuleBean service = bean("com.acme.OrderService", true, Stereotype.SERVICE);
        ModuleBean internal = bean("com.acme.internal.Repo", false, Stereotype.REPOSITORY);
        ModuleBean other = bean("com.acme.Other", true);
        ModuleDetails details = new ModuleDetails("order", Optional.empty(), List.of(), List.of(service, internal, other),
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());

        // act
        var api = CanvasOptions.defaults().groupBeans(details);
        var all = CanvasOptions.defaults().revealInternals().groupBeans(details);

        // assert
        assertThat(api).hasSize(2);
        assertThat(api.keySet()).extracting(Grouping::getName).containsExactly("Services", "Others");
        assertThat(all.keySet()).extracting(Grouping::getName).containsExactly("Services", "Repositories", "Others");
        assertThat(all.get(CanvasOptions.FALLBACK_GROUP)).containsExactly(other);
    }

    @Test
    void quarkusAndSpringGroupsUseTheirOwnNames() {
        // arrange
        ModuleBean bean = bean("com.acme.All", true, Stereotype.values());

        // act
        List<String> quarkus = Stream.of(QuarkusGroupings.getGroupings()).map(Grouping::getName).toList();
        List<String> spring = Stream.of(SpringGroupings.getGroupings()).map(Grouping::getName).toList();
        boolean allMatch = Stream.concat(Stream.of(QuarkusGroupings.getGroupings()),
                Stream.of(SpringGroupings.getGroupings())).allMatch(it -> it.matches(bean));
        boolean noneMatch = Stream.concat(Stream.of(QuarkusGroupings.getGroupings()),
                Stream.of(SpringGroupings.getGroupings())).noneMatch(it -> it.matches(bean("com.acme.None", true)));

        // assert
        assertThat(quarkus).containsExactly("REST resources", "Services", "Repositories", "Event listeners",
                "Configuration mappings", "Scheduled jobs", "Messaging endpoints");
        assertThat(spring).containsExactly("Controllers", "Services", "Repositories", "Event listeners",
                "Configuration properties");
        assertThat(allMatch).isTrue();
        assertThat(noneMatch).isTrue();
    }

    static Stream<Arguments> jMoleculesGroups() {
        Grouping[] groupings = JMoleculesGroupings.getGroupings();
        List<String> annotations = List.of("cqrs.Command", "cqrs.CommandDispatcher", "cqrs.CommandHandler",
                "cqrs.QueryModel", "hexagonal.PrimaryPort", "hexagonal.SecondaryPort", "hexagonal.Port",
                "hexagonal.Application", "hexagonal.PrimaryAdapter", "hexagonal.SecondaryAdapter", "hexagonal.Adapter",
                "layered.ApplicationLayer", "layered.DomainLayer", "layered.InfrastructureLayer",
                "layered.InterfaceLayer", "onion.simplified.ApplicationRing", "onion.simplified.DomainRing",
                "onion.simplified.InfrastructureRing", "onion.classical.ApplicationServiceRing",
                "onion.classical.DomainServiceRing", "onion.classical.DomainModelRing",
                "onion.classical.InfrastructureRing");
        return IntStream.range(0, groupings.length)
                .mapToObj(i -> Arguments.of(groupings[i], "org.jmolecules.architecture." + annotations.get(i)));
    }

    @ParameterizedTest
    @MethodSource("jMoleculesGroups")
    void jMoleculesGroupsMatchTypePackageAndMetaAnnotations(Grouping grouping, String annotation) {
        // arrange
        ModuleBean direct = annotated(Set.of(annotation), Set.of(), Set.of());
        ModuleBean meta = annotated(Set.of(), Set.of(annotation), Set.of());
        ModuleBean inPackage = annotated(Set.of(), Set.of(), Set.of(annotation));
        ModuleBean none = annotated(Set.of("x.Y"), Set.of("x.Z"), Set.of("x.W"));

        // act
        List<Boolean> matches = Stream.of(direct, meta, inPackage, none).map(grouping::matches).toList();

        // assert
        assertThat(matches).containsExactly(true, true, true, false);
    }

    @Test
    void documenterOptionsChooseTheOutput(@TempDir Path temp) {
        // arrange
        DocumentationOutput output = new FolderOutput(temp);

        // act
        DocumenterOptions defaults = DocumenterOptions.defaults();
        DocumenterOptions folder = defaults.withOutputFolder(temp.toString()).withoutClean();
        DocumenterOptions reset = folder.withOutputFolder(null);
        DocumenterOptions custom = defaults.withOutput(output);

        // assert
        assertThat(defaults.clean()).isTrue();
        assertThat(defaults.output()).hasToString(DocumenterOptions.DEFAULT_LOCATION);
        assertThat(folder.clean()).isFalse();
        assertThat(folder.output()).hasToString(temp.toString());
        assertThat(reset.output()).hasToString(DocumenterOptions.DEFAULT_LOCATION);
        assertThat(reset.clean()).isFalse();
        assertThat(custom.output()).isSameAs(output);
        assertThat(DocumenterOptions.DEFAULT_LOCATION).isEqualTo("target/modulith-docs");
    }

    @ParameterizedTest
    @CsvSource({ "true, target/modulith-docs, target/classes", "false, build/modulith-docs, build/resources/main" })
    void buildFoldersFollowTheBuildTool(boolean maven, String location, String resourceFolder, @TempDir Path temp)
            throws IOException {
        // arrange
        if (maven) {
            Files.writeString(temp.resolve("pom.xml"), "<project/>");
        }

        // act
        String actualLocation = DocumenterOptions.location(temp);
        String actualResourceFolder = Documenter.resourceFolder(temp);

        // assert
        assertThat(actualLocation).isEqualTo(location);
        assertThat(actualResourceFolder).isEqualTo(resourceFolder);
    }

    static ModuleBean bean(String name, boolean api, Stereotype... stereotypes) {
        return new ModuleBean(ModuleType.of(name), api, List.of(), Set.of(stereotypes), List.of());
    }

    static ModuleBean annotated(Set<String> annotations, Set<String> meta, Set<String> packages) {
        return new ModuleBean(new ModuleType("com.acme.Bean", true, true, annotations, meta, packages, Set.of()), true,
                List.of(), Set.of(), List.of());
    }
}
