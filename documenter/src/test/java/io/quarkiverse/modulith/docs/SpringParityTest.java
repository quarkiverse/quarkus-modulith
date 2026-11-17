package io.quarkiverse.modulith.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.modulith.core.util.ApplicationModulesExporter;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.JsonPath;

import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.fixtures.edge.EdgeApplication;
import io.quarkiverse.modulith.docs.fixtures.shop.ShopApplication;
import io.quarkiverse.modulith.docs.fixtures.shop.inventory.Stock;

/**
 * Runs Spring Modulith 2.1.2's {@code Documenter} on Spring Modulith's analysis of a fixture, and the Quarkus one on
 * the Quarkus Modulith core's analysis of the same fixture, and compares every file. Documented differences are normalized:
 * Spring Modulith orders relationships and listener methods by hash
 * codes, so relationship lines and listened events are compared sorted, and the canvas row "Spring components" is
 * called "Components".
 */
class SpringParityTest {

    static final List<ModuleProperty> PROPERTIES = List.of(
            new ModuleProperty("shop.order.limit", "java.lang.Integer",
                    "io.quarkiverse.modulith.docs.fixtures.shop.order.OrderProperties",
                    Optional.of("The maximum number of open orders, see {@link "
                            + "io.quarkiverse.modulith.docs.fixtures.shop.order.Order}."),
                    Optional.of("5")),
            new ModuleProperty("shop.order.region", "java.lang.String",
                    "io.quarkiverse.modulith.docs.fixtures.shop.order.OrderProperties", Optional.empty(),
                    Optional.empty()));

    /**
     * Spring Modulith reads the Javadoc only from {@code target/generated-spring-modulith/javadoc.json} of the working
     * directory.
     */
    @BeforeAll
    static void writeSpringJavadoc() throws IOException {
        Path target = Path.of("target/generated-spring-modulith/javadoc.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, ParityFixture.JAVADOC);
    }

    static Stream<Arguments> diagramCases() {
        List<String> cases = List.of("defaults", "uml", "color", "uml-color", "skin", "uml-skin", "listeners",
                "all", "none", "exclusions", "target-only", "visible", "names", "file-name", "filter", "exclude-all",
                "uml-exclude-all");
        return Stream.of(ShopApplication.class, EdgeApplication.class)
                .flatMap(app -> cases.stream().map(it -> Arguments.of(app, it)));
    }

    static Stream<Arguments> canvasCases() {
        List<String> cases = List.of("defaults", "internals", "empty-lines", "api-base", "no-groupings", "custom",
                "file-name");
        return Stream.of(ShopApplication.class, EdgeApplication.class)
                .flatMap(app -> cases.stream().map(it -> Arguments.of(app, it)));
    }

    @ParameterizedTest
    @MethodSource("diagramCases")
    void writesTheSameDiagrams(Class<?> application, String option, @TempDir Path temp) {
        // arrange
        ParityFixture fixture = new ParityFixture(application);

        // act
        fixture.spring(temp.resolve("spring"), springDiagram(option), springCanvas("defaults"));
        fixture.quarkus(temp.resolve("quarkus"), diagram(option), canvas("defaults"));

        // assert
        assertThat(read(temp.resolve("quarkus"))).isEqualTo(read(temp.resolve("spring")));
    }

    @ParameterizedTest
    @MethodSource("canvasCases")
    void writesTheSameCanvases(Class<?> application, String option, @TempDir Path temp) {
        // arrange
        ParityFixture fixture = new ParityFixture(application);

        // act
        fixture.spring(temp.resolve("spring"), springDiagram("defaults"), springCanvas(option));
        fixture.quarkus(temp.resolve("quarkus"), diagram("defaults"), canvas(option));

        // assert
        assertThat(read(temp.resolve("quarkus"))).isEqualTo(read(temp.resolve("spring")));
    }

    @ParameterizedTest
    @MethodSource("applications")
    void writesTheSameModuleMetadata(Class<?> application) {
        // arrange
        ParityFixture fixture = new ParityFixture(application);

        // act
        String simple = ApplicationModulesJson.simple(fixture.model);
        String full = ApplicationModulesJson.full(fixture.model);

        // assert
        assertThat(json(simple)).isEqualTo(json(new ApplicationModulesExporter(fixture.spring).toJson()));
        assertThat(json(full)).isEqualTo(json(new ApplicationModulesExporter(fixture.spring).toFullJson()));
    }

    static Stream<Class<?>> applications() {
        return Stream.of(ShopApplication.class, EdgeApplication.class);
    }

    static DiagramOptions diagram(String option) {
        DiagramOptions defaults = DiagramOptions.defaults();
        return switch (option) {
            case "uml" -> defaults.withStyle(DiagramStyle.UML);
            case "color" -> defaults.withColorSelector(SpringParityTest::color);
            case "uml-color" -> defaults.withStyle(DiagramStyle.UML).withColorSelector(SpringParityTest::color);
            case "skin" -> defaults.withSkinParam("linetype", "ortho").withSkinParam("shadowing", null);
            case "uml-skin" -> defaults.withStyle(DiagramStyle.UML).withSkinParam("linetype", "ortho")
                    .withSkinParam("componentStyle", "rectangle");
            case "listeners" -> defaults.withDependencyTypes(DependencyType.EVENT_LISTENER);
            case "all" -> defaults.withDependencyDepth(DependencyDepth.ALL);
            case "none" -> defaults.withDependencyDepth(DependencyDepth.NONE);
            case "exclusions" -> defaults.withExclusions(it -> it.getName().equals("catalog"));
            case "target-only" -> defaults.withTargetOnly(it -> it.getName().equals("order"));
            case "visible" -> defaults.withElementsWithoutRelationships(ElementsWithoutRelationships.VISIBLE);
            case "names" -> defaults.withDefaultDisplayName(it -> it.getName().toUpperCase());
            case "file-name" -> defaults.withTargetFileName("diagram-%s.puml");
            case "filter" -> defaults.withComponentFilter(it -> !it.getName().equals("Inventory"));
            case "exclude-all" -> defaults.withExclusions(it -> true);
            case "uml-exclude-all" -> defaults.withStyle(DiagramStyle.UML).withExclusions(it -> true);
            default -> defaults;
        };
    }

    static org.springframework.modulith.docs.Documenter.DiagramOptions springDiagram(String option) {
        org.springframework.modulith.docs.Documenter.DiagramOptions defaults = org.springframework.modulith.docs.Documenter.DiagramOptions
                .defaults();
        org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle uml = org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle.UML;
        return switch (option) {
            case "uml" -> defaults.withStyle(uml);
            case "color" -> defaults.withColorSelector(it -> color(it.getIdentifier().toString()));
            case "uml-color" -> defaults.withStyle(uml).withColorSelector(it -> color(it.getIdentifier().toString()));
            case "skin" -> defaults.withSkinParam("linetype", "ortho").withSkinParam("shadowing", null);
            case "uml-skin" -> defaults.withStyle(uml).withSkinParam("linetype", "ortho")
                    .withSkinParam("componentStyle", "rectangle");
            case "listeners" -> defaults.withDependencyTypes(
                    org.springframework.modulith.core.DependencyType.EVENT_LISTENER);
            case "all" -> defaults.withDependencyDepth(org.springframework.modulith.core.DependencyDepth.ALL);
            case "none" -> defaults.withDependencyDepth(org.springframework.modulith.core.DependencyDepth.NONE);
            case "exclusions" -> defaults.withExclusions(it -> it.getIdentifier().toString().equals("catalog"));
            case "target-only" -> defaults.withTargetOnly(it -> it.getIdentifier().toString().equals("order"));
            case "visible" -> defaults.withElementsWithoutRelationships(
                    org.springframework.modulith.docs.Documenter.DiagramOptions.ElementsWithoutRelationships.VISIBLE);
            case "names" -> defaults.withDefaultDisplayName(it -> it.getIdentifier().toString().toUpperCase());
            case "file-name" -> defaults.withTargetFileName("diagram-%s.puml");
            case "filter" -> defaults.withComponentFilter(it -> !it.getName().equals("Inventory"));
            case "exclude-all" -> defaults.withExclusions(it -> true);
            case "uml-exclude-all" -> defaults.withStyle(uml).withExclusions(it -> true);
            default -> defaults;
        };
    }

    static CanvasOptions canvas(String option) {
        CanvasOptions spring = CanvasOptions.withoutDefaultGroupings().groupingBy(JMoleculesGroupings.getGroupings())
                .groupingBy(SpringGroupings.getGroupings());
        return switch (option) {
            case "internals" -> spring.revealInternals();
            case "empty-lines" -> spring.revealEmptyLines();
            case "api-base" -> spring.withApiBase("https://docs.example/api");
            case "no-groupings" -> CanvasOptions.withoutDefaultGroupings();
            case "custom" -> CanvasOptions.withoutDefaultGroupings()
                    .groupingBy("Adapters", Grouping.nameMatching(".*Adapter"), "Talk to the outside")
                    .groupingBy("Stock", Grouping.implementing(Stock.class))
                    .groupingBy("Stock subtypes", Grouping.subtypeOf(Stock.class))
                    .groupingBy(Grouping.of("Services", Grouping.isAnnotatedWith(Service.class)))
                    .revealInternals();
            case "file-name" -> spring.withTargetFileName("canvas-%s.adoc");
            default -> spring;
        };
    }

    static org.springframework.modulith.docs.Documenter.CanvasOptions springCanvas(String option) {
        org.springframework.modulith.docs.Documenter.CanvasOptions spring = org.springframework.modulith.docs.Documenter.CanvasOptions
                .defaults();
        return switch (option) {
            case "internals" -> spring.revealInternals();
            case "empty-lines" -> spring.revealEmptyLines();
            case "api-base" -> spring.withApiBase("https://docs.example/api");
            case "no-groupings" -> org.springframework.modulith.docs.Documenter.CanvasOptions.withoutDefaultGroupings();
            case "custom" -> org.springframework.modulith.docs.Documenter.CanvasOptions.withoutDefaultGroupings()
                    .groupingBy("Adapters",
                            org.springframework.modulith.docs.Documenter.CanvasOptions.Grouping.nameMatching(".*Adapter"),
                            "Talk to the outside")
                    .groupingBy("Stock",
                            org.springframework.modulith.docs.Documenter.CanvasOptions.Grouping.implementing(Stock.class))
                    .groupingBy("Stock subtypes",
                            org.springframework.modulith.docs.Documenter.CanvasOptions.Grouping.subtypeOf(Stock.class))
                    .groupingBy(org.springframework.modulith.docs.Documenter.CanvasOptions.Grouping.of("Services",
                            org.springframework.modulith.docs.Documenter.CanvasOptions.Grouping.isAnnotatedWith(Service.class)))
                    .revealInternals();
            case "file-name" -> spring.withTargetFileName("canvas-%s.adoc");
            default -> spring;
        };
    }

    static Optional<String> color(io.quarkiverse.modulith.core.ApplicationModule module) {
        return color(module.getName());
    }

    static Optional<String> color(String module) {
        return switch (module) {
            case "order", "alpha" -> Optional.of("#FF0000");
            case "inventory", "beta" -> Optional.of("SteelBlue");
            default -> Optional.empty();
        };
    }

    static Map<String, String> read(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            Map<String, String> result = new TreeMap<>();
            for (Path file : files.toList()) {
                result.put(file.getFileName().toString(), normalize(Files.readString(file)));
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String normalize(String content) {
        List<String> lines = new ArrayList<>(List.of(content.split("\n", -1)));
        List<Integer> relationships = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Rel(") || lines.get(i).contains(" --> ")) {
                relationships.add(i);
            }
        }
        List<String> sorted = relationships.stream().map(lines::get).sorted().toList();
        for (int i = 0; i < relationships.size(); i++) {
            lines.set(relationships.get(i), sorted.get(i));
        }
        int listened = lines.indexOf("|Events listened to");
        if (listened >= 0) {
            int end = listened + 2;
            while (end < lines.size() && !lines.get(end).startsWith("|")) {
                end++;
            }
            List<String> block = new ArrayList<>(lines.subList(listened + 1, end));
            block.set(0, block.get(0).substring(1));
            block.sort(null);
            block.set(0, "|" + block.get(0));
            for (int i = 0; i < block.size(); i++) {
                lines.set(listened + 1 + i, block.get(i));
            }
        }
        return String.join("\n", lines).replace("|Spring components", "|Components");
    }

    /**
     * Parses JSON with lists turned into sets: Spring Modulith writes dependencies and types in hash order.
     */
    static Object json(String text) {
        Object parsed = JsonPath.parse(text).json();
        return unordered(parsed);
    }

    static Object unordered(Object json) {
        if (json instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(key, unordered(value)));
            return result;
        }
        if (json instanceof Collection<?> collection) {
            return new HashSet<>(collection.stream().map(SpringParityTest::unordered).toList());
        }
        return json;
    }
}
