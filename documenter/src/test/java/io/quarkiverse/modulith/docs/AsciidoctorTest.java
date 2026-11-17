package io.quarkiverse.modulith.docs;

import static io.quarkiverse.modulith.docs.TestModels.module;
import static io.quarkiverse.modulith.docs.TestModels.modules;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

class AsciidoctorTest {

    static final ApplicationModules MODULES = modules("Shop", module("order", List.of("catalog")),
            module("catalog", List.of()));
    static final ModuleType HIDDEN = new ModuleType("com.acme.order.internal.Hidden", false, false, Set.of(), Set.of(),
            Set.of(), Set.of());
    static final ModuleType ORDER = ModuleType.of("com.acme.order.Order");

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "#method|`method`",
            "''|''",
            "com.acme.order.Order#id|link:https://api/com/acme/order/Order.html[`c.a.o.Order#id`]",
            "com.acme.order.Unknown#id|`com.acme.order.Unknown`",
            "com.acme.order.internal.Hidden|`c.a.o.internal.Hidden`",
            "org.other.Type|`org.other.Type`" })
    void rendersInlineCode(String source, String expected) {
        // arrange
        Asciidoctor asciidoctor = new Asciidoctor(model(), Optional.of("https://api"));

        // act
        String code = asciidoctor.toInlineCode(source);

        // assert
        assertThat(code).isEqualTo(expected);
    }

    @Test
    void rendersBeanReferencesOncePerTarget() {
        // arrange
        Asciidoctor asciidoctor = new Asciidoctor(model(), Optional.empty());

        // act
        String references = asciidoctor.renderBeanReferences(order());

        // assert
        assertThat(references).isEqualTo("* `c.a.c.Catalog` (in CATALOG)" + Asciidoctor.NL
                + "* `o.o.Service` (in gone)");
    }

    @Test
    void rendersExposedEventsOnlyAndThoseWithoutSources() {
        // arrange
        ModuleDetails order = new ModuleDetails("order", Optional.empty(), List.of(ORDER, HIDDEN), List.of(), List.of(),
                List.of(new PublishedEvent(HIDDEN, List.of()), new PublishedEvent(ORDER, List.of())), List.of(),
                List.of(), List.of(), Map.of(), List.of());
        Asciidoctor asciidoctor = new Asciidoctor(
                new DocumentationModel(MODULES, List.of(order), List.of(), DocumentationSource.none()), Optional.empty());

        // act
        String events = asciidoctor.renderPublishedEvents(order());

        // assert
        assertThat(events).isEqualTo("* `c.a.o.Order`" + Asciidoctor.NL);
    }

    @Test
    void rendersListenersWithoutExposedEventsAsEmptyLines() {
        // arrange
        Asciidoctor asciidoctor = new Asciidoctor(model(), Optional.empty());

        // act
        String listeners = asciidoctor.renderEventsListenedTo(order());

        // assert
        assertThat(listeners).isEqualTo("" + Asciidoctor.NL + "* `c.a.o.Order` (async) ");
    }

    @Test
    void leavesOutBlankDefaultsAndDescriptions() {
        // arrange
        Asciidoctor asciidoctor = new Asciidoctor(model(), Optional.empty());
        List<ModuleProperty> properties = List.of(new ModuleProperty("a", "int", "com.acme.order.Config",
                Optional.of(" "), Optional.of("")));

        // act
        String rendered = asciidoctor.renderConfigurationProperties(properties);

        // assert
        assertThat(rendered).isEqualTo("* `a` -- `int`");
    }

    @Test
    void rendersNoneWithoutVisibleBeans() {
        // arrange
        Asciidoctor asciidoctor = new Asciidoctor(model(), Optional.empty());

        // act
        String beans = asciidoctor.renderBeans(model().details(order()), CanvasOptions.defaults());

        // assert
        assertThat(beans).isEqualTo("None");
    }

    @Test
    void showsOnlyExposedTypesUnlessInternalsAreRevealed() {
        // arrange
        CanvasOptions hidden = CanvasOptions.defaults();
        CanvasOptions revealed = CanvasOptions.defaults().revealInternals();

        // act
        boolean hiddenShowsInternal = hidden.shows(HIDDEN);
        boolean hiddenShowsExposed = hidden.shows(ORDER);
        boolean revealedShowsInternal = revealed.shows(HIDDEN);

        // assert
        assertThat(hiddenShowsInternal).isFalse();
        assertThat(hiddenShowsExposed).isTrue();
        assertThat(revealedShowsInternal).isTrue();
    }

    static ApplicationModule order() {
        return MODULES.getModuleByName("order").orElseThrow();
    }

    static DocumentationModel model() {
        ModuleBean internal = new ModuleBean(HIDDEN, false, List.of(), Set.of(Stereotype.EVENT_LISTENER), List.of(
                new ListenerMethod("on", List.of(HIDDEN.name()), List.of(HIDDEN.name()), false),
                new ListenerMethod("on", List.of(ORDER.name()), List.of(ORDER.name()), true)));
        ModuleDetails order = new ModuleDetails("order", Optional.empty(), List.of(ORDER, HIDDEN), List.of(internal),
                List.of(
                        new ModuleDependency("com.acme.order.A", "com.acme.catalog.Catalog",
                                DependencyType.USES_COMPONENT, "catalog"),
                        new ModuleDependency("com.acme.order.B", "com.acme.catalog.Catalog",
                                DependencyType.USES_COMPONENT, "catalog"),
                        new ModuleDependency("com.acme.order.A", "org.other.Service", DependencyType.USES_COMPONENT,
                                "gone")),
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        return new DocumentationModel(MODULES, List.of(order), List.of(), DocumentationSource.none());
    }
}
