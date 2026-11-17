package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.ListenerMethod;
import io.quarkiverse.modulith.docs.ModuleBean;
import io.quarkiverse.modulith.docs.ModuleDetails;
import io.quarkiverse.modulith.docs.ModuleType;
import io.quarkiverse.modulith.docs.Stereotype;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;
import io.quarkiverse.modulith.observability.ModuleInvocation;
import io.quarkiverse.modulith.observability.deployment.types.a.Base;
import io.quarkiverse.modulith.observability.deployment.types.a.Listener;
import io.quarkiverse.modulith.observability.deployment.types.a.Other;
import io.quarkiverse.modulith.observability.deployment.types.a.Service;
import io.quarkiverse.modulith.observability.deployment.types.b.Event;
import io.quarkiverse.modulith.observability.runtime.ObservedModules;

class ObservedTypesTest {

    static final String A = "io.quarkiverse.modulith.observability.deployment.types.a.";
    static final String B = "io.quarkiverse.modulith.observability.deployment.types.b.";
    static final String EVENT = Event.class.getName();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "controller|CONTROLLER|false|''|''|true",
            "messaging|MESSAGING_ENDPOINT|false|''|''|true",
            "otherModuleListener||false|" + "io.quarkiverse.modulith.observability.deployment.types.b.Event|''|true",
            "sameModuleListener||false|io.quarkiverse.modulith.observability.deployment.types.a.Service|''|false",
            "plainListener||false|java.lang.String|''|true",
            "exposed||true|''|''|true",
            "exposedSupertype||false|''|io.quarkiverse.modulith.observability.deployment.types.a.Api|true",
            "hiddenSupertype||false|''|io.quarkiverse.modulith.observability.deployment.types.a.Hidden|false",
            "foreignSupertype||false|''|java.io.Serializable|false",
            "plain||false|''|''|false" })
    void observesBeansLikeSpringModulith(String name, Stereotype stereotype, boolean exposed, String listened,
            String supertype, boolean observed) throws IOException {
        // arrange
        ModuleBean bean = new ModuleBean(
                new ModuleType(A + name, true, exposed, Set.of(), Set.of(), Set.of(),
                        supertype.isEmpty() ? Set.of() : Set.of(supertype)),
                true, List.of(), Optional.ofNullable(stereotype).map(Set::of).orElse(Set.of()),
                listened.isEmpty() ? List.of()
                        : List.of(new ListenerMethod("on", List.of(listened), List.of(listened),
                                false)));
        ObservedTypes types = types(List.of(bean));
        ApplicationModules modules = modules();

        // act
        boolean actual = types.isObserved(modules.getModuleByName("a").orElseThrow(),
                details(List.of(bean)), bean);

        // assert
        assertThat(actual).isEqualTo(observed);
    }

    @ParameterizedTest
    @CsvSource({ "Service, Event, true", "Event, Service, true", "Service, Base, false", "'', '', true" })
    void observesAListenerOfAnotherModulesEventWhateverTheOrderOfItsMethods(String first, String second,
            boolean observed) throws IOException {
        // arrange
        ModuleBean bean = new ModuleBean(new ModuleType(A + "TwoListeners", true, false, Set.of(), Set.of(), Set.of(),
                Set.of()), true, List.of(), Set.of(), List.of(listener(first), listener(second)));
        ObservedTypes types = types(List.of(bean));

        // act
        boolean actual = types.isObserved(modules().getModuleByName("a").orElseThrow(), details(List.of(bean)), bean);

        // assert
        assertThat(actual).isEqualTo(observed);
    }

    @Test
    void namesTheObservedMethods() throws IOException {
        // arrange
        ModuleBean service = bean(Service.class, List.of());
        ModuleBean listener = bean(Listener.class, List.of(new ListenerMethod("on", List.of(EVENT), List.of(EVENT),
                true)));
        ModuleBean other = bean(Other.class, List.of());

        // act
        ObservedTypesBuildItem item = types(List.of(service, listener, other)).compute();

        // assert
        assertThat(item.getClasses()).containsExactlyInAnyOrder(A + "Service", A + "Listener", A + "Other");
        String[] rows = item.getRows().toArray(String[]::new);
        assertThat(rows).hasSize(8);
        ObservedModules observed = ObservedModules.of(rows);
        assertThat(contextualName(observed, "Service", "overridden()"))
                .isEqualTo("Alpha > i.q.m.o.d.t.a.Service.overridden()");
        assertThat(contextualName(observed, "Service", "inherited()"))
                .isEqualTo("Alpha > i.q.m.o.d.t.a.Service.inherited()");
        assertThat(contextualName(observed, "Service", "parameters(java.lang.String[][],java.util.List,java.util.Map)"))
                .isEqualTo("Alpha > i.q.m.o.d.t.a.Service.parameters(j.l.String[][], j.u.List<?>, "
                        + "j.u.Map<j.l.String, i.q.m.o.d.t.b.Event>)");
        assertThat(contextualName(observed, "Service", "generic(java.lang.Object)"))
                .isEqualTo("Alpha > i.q.m.o.d.t.a.Service.generic(?)");
        assertThat(observed.find(A + "Service", "util()")).isEmpty();
        assertThat(observed.find(A + "Service", "produced()")).isEmpty();
        assertThat(observed.find(A + "Listener", "on(" + B + "Event)").orElseThrow().invocationType())
                .isEqualTo("event-listener");
        assertThat(observed.find(A + "Listener", "other(java.lang.String)").orElseThrow().invocationType())
                .isEqualTo("bean");
        assertThat(contextualName(observed, "Other", "own()")).isEqualTo("Alpha > i.q.m.o.d.t.a.Other.own()");
    }

    @Test
    void observesRestDataResources() throws IOException {
        // arrange
        ModuleType resource = new ModuleType(A + "ItemResource", true, true, Set.of(), Set.of(), Set.of(),
                Set.of("io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource"));
        ModuleType notResource = new ModuleType(A + "Lookalike", true, true, Set.of(), Set.of(), Set.of(),
                Set.of("io.quarkus.other.PanacheEntityResource", "com.acme.rest.data.panache.Resource",
                        "io.quarkus.hibernate.orm.rest.data.panache.Helper"));
        ModuleDetails details = new ModuleDetails("a", Optional.empty(), List.of(resource, notResource), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        DocumentationModel model = new DocumentationModel(modules(), List.of(details), List.of(),
                DocumentationSource.none());

        // act
        ObservedTypesBuildItem item = new ObservedTypes(model, Index.of(Object.class)).compute();

        // assert
        assertThat(item.getResources()).containsExactly(A + "ItemResource");
        assertThat(item.getClasses()).isEmpty();
        assertThat(item.getRows()).containsExactly(ObservedModules.resourceRow(A + "ItemResource",
                new ModuleInvocation("a", "Alpha", "", "bean",
                        "Alpha > i.q.m.o.d.t.a.ItemResource")));
    }

    static ListenerMethod listener(String type) {
        if (type.isEmpty()) {
            return new ListenerMethod("on", List.of(), List.of(), false);
        }
        String name = (type.equals("Event") ? B : A) + type;
        return new ListenerMethod("on", List.of(name), List.of(name), false);
    }

    static String contextualName(ObservedModules observed, String type, String signature) {
        return observed.find(A + type, signature).orElseThrow().contextualName();
    }

    static ModuleBean bean(Class<?> type, List<ListenerMethod> listeners) {
        return new ModuleBean(new ModuleType(type.getName(), true, true, Set.of(), Set.of(), Set.of(), Set.of()), true,
                List.of(), Set.of(), listeners);
    }

    static ObservedTypes types(List<ModuleBean> beans) throws IOException {
        Index index = Index.of(Object.class, Base.class, Service.class, Other.class, Listener.class, Event.class);
        DocumentationModel model = new DocumentationModel(modules(), List.of(details(beans),
                new ModuleDetails("b", Optional.empty(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(), Map.of(), List.of())),
                List.of(), DocumentationSource.none());
        return new ObservedTypes(model, index);
    }

    static ModuleDetails details(List<ModuleBean> beans) {
        List<ModuleType> types = List.of(
                new ModuleType(A + "Api", true, true, Set.of(), Set.of(), Set.of(), Set.of()),
                new ModuleType(A + "Hidden", true, false, Set.of(), Set.of(), Set.of(), Set.of()));
        return new ModuleDetails("a", Optional.empty(), types, beans, List.of(), List.of(), List.of(), List.of(),
                List.of(), Map.of(), List.of());
    }

    static ApplicationModules modules() {
        return new ApplicationModules(new ModelData("Types", "io.quarkiverse.modulith.observability.deployment.types",
                List.of(module("a", "Alpha"), module("b", "Beta")), List.of(), ""));
    }

    static ModelData.ModuleData module(String name, String displayName) {
        return new ModelData.ModuleData(name, displayName, "io.quarkiverse.modulith.observability.deployment.types." + name,
                false, false, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }
}
