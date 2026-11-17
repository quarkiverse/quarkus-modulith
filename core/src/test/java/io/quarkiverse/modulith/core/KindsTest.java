package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Stereotypes, beans and listeners of the {@code fixtures.kinds} types, one variant per type.
 */
class KindsTest {

    static final String K = "io.quarkiverse.modulith.core.fixtures.kinds";
    static final ApplicationModules MODULES = ApplicationModules.of(K, ClassLocations.TESTS);

    static List<String> stereotypes(String type) {
        return MODULES.getModelData().types().stream().filter(it -> it.name().equals(K + "." + type)).findFirst()
                .orElseThrow().stereotypes();
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "k.KEntity;ENTITY,AGGREGATE_ROOT", "k.SpringRepo;REPOSITORY,INJECTABLE",
            "k.DefinedRepo;INJECTABLE", "k.JEntity;ENTITY", "k.JAggregate;ENTITY,AGGREGATE_ROOT", "k.JValue;VALUE_OBJECT",
            "k.ScopedInterface;INJECTABLE", "k.JService;SERVICE,INJECTABLE", "k.PathInterface;CONTROLLER,INJECTABLE",
            "k.Handler;EVENT_LISTENER,INJECTABLE", "k.SpringListener;EVENT_LISTENER,INJECTABLE", "k.Plain;",
            "other.Child;", "k.PanacheOnly;ENTITY,AGGREGATE_ROOT", "k.RouteOnly;CONTROLLER,INJECTABLE",
            "k.BothStereotypes;SERVICE,INJECTABLE", "k.internal.Impl;INJECTABLE" })
    void classifiesTypes(String type, String expected) {
        // act
        List<String> result = stereotypes(type);

        // assert
        assertThat(result).isEqualTo(expected == null ? List.of() : List.of(expected.split(",")));
    }

    @Test
    void describesListenerMethods() {
        // act
        List<ReferenceMethod> methods = MODULES.getModuleByName("k").orElseThrow().getType("Handler")
                .map(ArchitecturallyEvidentType::of).orElseThrow().getReferenceMethods().toList();

        // assert
        assertThat(methods)
                .extracting(ReferenceMethod::getName, ReferenceMethod::isAsync, ReferenceMethod::getReferenceTypeNames)
                .containsExactly(tuple("handle", false, List.of(K + ".k.KEvent")),
                        tuple("handle", false, List.of(K + ".other.Other", "int")),
                        tuple("withoutParameters", true, List.of(K + ".k.KEntity")));
    }

    @Test
    void describesEntryPoints() {
        // act
        List<EntryPoint> entryPoints = MODULES.getModuleByName("k").orElseThrow().getEntryPoints();

        // assert
        assertThat(entryPoints).extracting(EntryPoint::method, EntryPoint::detail).containsExactly(
                tuple("handle", K + ".k.KEvent"), tuple("handle", K + ".other.Other, int"),
                tuple("withoutParameters", K + ".k.KEntity"), tuple("tick", ""), tuple("get", "GET /plain"),
                tuple("route", "path=/r methods=GET"),
                tuple("onApplicationEvent", "org.springframework.context.event.ContextRefreshedEvent"));
    }

    @Test
    void findsBeansEventsAndInjectionMethods() {
        // arrange
        ApplicationModule module = MODULES.getModuleByName("k").orElseThrow();

        // act
        List<String> beans = module.getBeans().stream().map(ModuleBean::getFullyQualifiedTypeName).toList();

        // assert
        assertThat(beans).containsExactly(K + ".k.BothStereotypes", K + ".k.DefinedRepo", K + ".k.SpringRepo",
                K + ".k.internal.Impl");
        assertThat(module.getPublishedEvents()).containsExactly(K + ".k.KEvent");
        assertThat(module.getPublishedEventTypes()).singleElement().satisfies(it -> assertThat(it.getSources())
                .extracting(source -> source.toString(module)).containsExactly(K + ".other.Other.<init>()"));
        assertThat(module.getDirectDependencies(MODULES, DependencyType.USES_COMPONENT).stream())
                .extracting(ApplicationModuleDependency::getDescription)
                .containsExactly("Fired declares injection method @Inject Fired.setOther(Other) in (Fired.java:0)");
        assertThat(MODULES.getModuleByName("other").orElseThrow().getBeans()).isEmpty();
        assertThat(module.getBeans().get(0)).isEqualTo(module.getBeans().get(0))
                .hasSameHashCodeAs(module.getBeans().get(0)).isNotEqualTo(module.getBeans().get(1)).isNotEqualTo("bean");
        assertThat(module.getBeans().get(0).getInterfacesWithinModule()).isEmpty();
        assertThat(module.getBeans().get(3).getInterfacesWithinModule()).extracting(JavaType::getSimpleName)
                .containsExactly("Contract2");
        assertThat(module.getBeans().get(3).isApiBean()).isTrue();
        assertThat(module.getNamedInterfaceDetails().getByName("metaapi").orElseThrow()).extracting(JavaType::getSimpleName)
                .containsExactly("Exposed", "MetaApi");
        assertThat(module.getNamedInterfaceDetails().getByName("loop").orElseThrow()).extracting(JavaType::getSimpleName)
                .containsExactly("Loop");
        assertThat(MODULES.getModelData().types()).filteredOn(it -> it.name().endsWith("BothStereotypes"))
                .singleElement().satisfies(it -> assertThat(it.metaAnnotations()).doesNotContain(
                        "org.springframework.stereotype.Component"));
    }
}
