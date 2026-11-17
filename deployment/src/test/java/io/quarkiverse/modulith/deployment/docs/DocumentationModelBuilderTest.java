package io.quarkiverse.modulith.deployment.docs;

import static io.quarkiverse.modulith.deployment.docs.DocsFixture.BASE;
import static io.quarkiverse.modulith.deployment.docs.DocsFixture.ORDER;
import static io.quarkiverse.modulith.deployment.docs.DocsFixture.WEB;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.EventSource;
import io.quarkiverse.modulith.docs.ListenerMethod;
import io.quarkiverse.modulith.docs.ModuleBean;
import io.quarkiverse.modulith.docs.ModuleDependency;
import io.quarkiverse.modulith.docs.ModuleDetails;
import io.quarkiverse.modulith.docs.ModuleProperty;
import io.quarkiverse.modulith.docs.ModuleType;
import io.quarkiverse.modulith.docs.Stereotype;

class DocumentationModelBuilderTest {

    static final String INVENTORY = BASE + ".inventory.";
    static final String CATALOG = BASE + ".catalog.";

    static DocumentationModel model;

    @BeforeAll
    static void build() {
        model = new DocsFixture().build();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "web|Resource|CONTROLLER",
            "web|Routes|CONTROLLER",
            "web|RouteBased|CONTROLLER",
            "web|MethodPath|CONTROLLER",
            "web|Jobs|SCHEDULED_JOB",
            "web|Consumer|MESSAGING_ENDPOINT",
            "web|Producer|MESSAGING_ENDPOINT",
            "web|Billing|SERVICE",
            "web|MetaService|SERVICE",
            "web|Repo1|REPOSITORY",
            "web|Repo2|REPOSITORY",
            "web|Repo3|REPOSITORY",
            "web|SpringListener|EVENT_LISTENER",
            "order|OrderService|SERVICE",
            "order|OrderConfig|CONFIGURATION_PROPERTIES",
            "order|Settings|CONFIGURATION_PROPERTIES",
            "inventory|InventoryListener|EVENT_LISTENER",
            "inventory|InventoryGateway|",
            "inventory|Stock|",
            "catalog|Catalog|" })
    void classifiesBeans(String module, String simpleName, Stereotype stereotype) {
        // arrange
        ModuleDetails details = details(module);

        // act
        ModuleBean bean = bean(details, simpleName);

        // assert
        assertThat(bean.stereotypes()).isEqualTo(Optional.ofNullable(stereotype).map(Set::of).orElse(Set.of()));
    }

    @Test
    void leavesOutTypesThatAreNoBeans() {
        // arrange
        List<String> notBeans = List.of(WEB + "Item", WEB + "MyService", WEB + "Plain", BASE + ".inventory.AbstractPart",
                INVENTORY + "Gateway", ORDER + "OrderView", ORDER + "OrderCompleted", CATALOG + "spi.Pricing");

        // act
        List<String> beans = model.modules().stream().flatMap(it -> model.details(it).beans().stream())
                .map(it -> it.type().name()).toList();

        // assert
        assertThat(beans).doesNotContainAnyElementsOf(notBeans).hasSize(28);
    }

    @Test
    void describesTypes() {
        // arrange
        ModuleDetails inventory = details("inventory");
        ModuleDetails catalog = details("catalog");

        // act
        ModuleType gateway = inventory.type(INVENTORY + "InventoryGateway").orElseThrow();
        ModuleType pricing = catalog.type(CATALOG + "spi.Pricing").orElseThrow();
        ModuleType implementation = catalog.type(CATALOG + "internal.DefaultPricing").orElseThrow();
        ModuleType hidden = details("order").type(ORDER + "Hidden").orElseThrow();

        // assert
        assertThat(gateway.annotations()).containsExactly(INVENTORY + "Gateway");
        assertThat(gateway.metaAnnotations()).containsExactlyInAnyOrder("jakarta.enterprise.inject.Stereotype",
                "jakarta.enterprise.context.ApplicationScoped", "java.lang.annotation.Retention");
        assertThat(pricing.packageAnnotations()).containsExactly("io.quarkiverse.modulith.NamedInterface");
        assertThat(pricing.exposed()).isTrue();
        assertThat(implementation.exposed()).isFalse();
        assertThat(implementation.supertypes()).containsExactlyInAnyOrder("java.lang.Object", CATALOG + "spi.Pricing");
        assertThat(hidden.publicType()).isFalse();
        assertThat(details("web").type(WEB + "Repo3").orElseThrow().supertypes()).containsExactlyInAnyOrder(
                "io.quarkus.hibernate.orm.panache.PanacheRepositoryBase", "java.io.Serializable", "java.lang.Object");
    }

    @Test
    void findsApiBeansThroughTheirInterfaces() {
        // arrange
        ModuleDetails order = details("order");
        ModuleDetails catalog = details("catalog");

        // act
        ModuleBean view = bean(order, "OrderViewImpl");
        ModuleBean store = bean(order, "OrderStore");
        ModuleBean pricing = bean(catalog, "DefaultPricing");

        // assert
        assertThat(view.apiBean()).isTrue();
        assertThat(view.interfacesWithinModule()).extracting(ModuleType::name).containsExactly(ORDER + "OrderView");
        assertThat(store.apiBean()).isFalse();
        assertThat(store.interfacesWithinModule()).isEmpty();
        assertThat(pricing.interfacesWithinModule()).extracting(ModuleType::name)
                .containsExactly(CATALOG + "spi.Pricing");
    }

    @Test
    void listsListenerMethods() {
        // arrange
        ModuleDetails inventory = details("inventory");
        ModuleDetails web = details("web");
        String completed = ORDER + "OrderCompleted";
        String cancelled = ORDER + "OrderCancelled";

        // act
        List<ListenerMethod> listeners = bean(inventory, "InventoryListener").listenerMethods();
        List<ListenerMethod> spring = bean(web, "SpringListener").listenerMethods();

        // assert
        assertThat(listeners).containsExactly(
                new ListenerMethod("observe", List.of(completed), List.of(completed), false),
                new ListenerMethod("observeAsync", List.of("jakarta.enterprise.inject.spi.BeanManager", cancelled),
                        List.of(cancelled), true),
                new ListenerMethod("on", List.of(completed), List.of(completed), true),
                new ListenerMethod("sync", List.of(cancelled), List.of(cancelled), false));
        assertThat(spring).containsExactly(
                new ListenerMethod("async", List.of(completed), List.of(completed), true),
                new ListenerMethod("module", List.of(completed), List.of(completed), true),
                new ListenerMethod("observed", List.of(completed), List.of(completed), true),
                new ListenerMethod("plain", List.of(completed, "java.lang.String[]"), List.of(completed), false));
    }

    @Test
    void typesDependencies() {
        // arrange
        String service = ORDER + "OrderService";

        // act
        List<ModuleDependency> order = details("order").dependencies();
        List<ModuleDependency> inventory = details("inventory").dependencies();
        List<ModuleDependency> web = details("web").dependencies();

        // assert
        assertThat(order).containsExactly(
                new ModuleDependency(service, CATALOG + "Catalog", DependencyType.USES_COMPONENT, "catalog"),
                new ModuleDependency(ORDER + "Timeouts", CATALOG + "Catalog", DependencyType.USES_COMPONENT, "catalog"),
                new ModuleDependency(service, CATALOG + "Catalog", DependencyType.DEFAULT, "catalog"),
                new ModuleDependency(service, CATALOG + "spi.Pricing", DependencyType.DEFAULT, "catalog"),
                new ModuleDependency(ORDER + "Timeouts", CATALOG + "Catalog", DependencyType.DEFAULT, "catalog"),
                new ModuleDependency(ORDER + "Timeouts", CATALOG + "Catalog", DependencyType.DEFAULT, "catalog"));
        assertThat(inventory.stream().distinct()).containsExactly(
                new ModuleDependency(INVENTORY + "Stock", CATALOG + "Catalog", DependencyType.USES_COMPONENT, "catalog"),
                new ModuleDependency(INVENTORY + "Stock", CATALOG + "Catalog", DependencyType.DEFAULT, "catalog"),
                new ModuleDependency(INVENTORY + "Stock", service, DependencyType.USES_COMPONENT, "order"),
                new ModuleDependency(INVENTORY + "InventoryListener", ORDER + "OrderCancelled",
                        DependencyType.EVENT_LISTENER, "order"),
                new ModuleDependency(INVENTORY + "InventoryListener", ORDER + "OrderCompleted",
                        DependencyType.EVENT_LISTENER, "order"),
                new ModuleDependency(INVENTORY + "InventoryListener", ORDER + "OrderCancelled", DependencyType.DEFAULT,
                        "order"),
                new ModuleDependency(INVENTORY + "InventoryListener", ORDER + "OrderCompleted", DependencyType.DEFAULT,
                        "order"),
                new ModuleDependency(INVENTORY + "Stock", service, DependencyType.DEFAULT, "order"));
        assertThat(web.stream().distinct()).containsExactly(
                new ModuleDependency(WEB + "Item", ORDER + "ItemOwner", DependencyType.ENTITY, "order"),
                new ModuleDependency(WEB + "SpringListener", ORDER + "OrderCompleted", DependencyType.EVENT_LISTENER,
                        "order"),
                new ModuleDependency(WEB + "SpringListener", ORDER + "OrderCompleted", DependencyType.DEFAULT,
                        "order"));
    }

    @Test
    void findsTheMethodsCreatingEvents() {
        // arrange
        ModuleDetails order = details("order");

        // act
        Map<String, List<EventSource>> events = order.publishedEvents().stream()
                .collect(Collectors.toMap(it -> it.type().name(), it -> it.sources()));

        // assert
        assertThat(events).containsOnly(
                Map.entry(ORDER + "OrderCancelled", List.of(new EventSource("i.q.m.d.d.f.a.o.OrderService", "cancel", true))),
                Map.entry(ORDER + "OrderCompleted",
                        List.of(new EventSource("i.q.m.d.d.f.a.o.OrderService", "complete", false))),
                Map.entry(ORDER + "internal.InternalEvent", List.of()));
        assertThat(order.publishedEvents().get(2).type().exposed()).isFalse();
        assertThat(details("web").publishedEvents()).isEmpty();
        assertThat(details("catalog").publishedEvents()).isEmpty();
    }

    @Test
    void findsAggregatesValueTypesAndInitializers() {
        // arrange
        ModuleDetails web = details("web");

        // act
        List<String> aggregates = web.aggregateRoots().stream().map(ModuleType::name).toList();
        List<String> values = web.valueTypes().stream().map(ModuleType::name).toList();

        // assert
        assertThat(aggregates).containsExactly(WEB + "Active", WEB + "Item", WEB + "Root", WEB + "Root2");
        assertThat(values).containsExactly(WEB + "Embedded", WEB + "Value", WEB + "Value2");
        assertThat(details("order").initializers()).containsExactly(ORDER + "OrderInit");
    }

    @Test
    void listsConfigurationProperties() {
        // arrange
        ModuleDetails order = details("order");
        String config = ORDER + "OrderConfig";

        // act
        List<ModuleProperty> properties = order.properties();

        // assert
        assertThat(properties).containsExactly(
                new ModuleProperty(ORDER + "Settings.blank", "java.lang.String", ORDER + "Settings", Optional.empty(),
                        Optional.empty()),
                new ModuleProperty(ORDER + "Settings.unnamed", "int", ORDER + "Settings", Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.explicit", "java.lang.String", ORDER + "Settings", Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.flag", "boolean", ORDER + "Settings", Optional.empty(), Optional.of("true")),
                new ModuleProperty("shop.order.backup.daily", "int", config + "$Limits", Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.order.limits.daily", "int", config + "$Limits", Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.order.max-size", "int", config, Optional.of("At most."), Optional.of("5")),
                new ModuleProperty("shop.order.mode", "java.lang.String", config + "$Extra", Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.order.note", "java.util.Optional<java.lang.String>", config, Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.order.region-code", "java.lang.String", config, Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.order.tags", "java.util.List<java.lang.String>", config, Optional.empty(),
                        Optional.empty()),
                new ModuleProperty("shop.retry", "int", ORDER + "Timeouts", Optional.empty(), Optional.of("3")),
                new ModuleProperty("shop.timeout", "java.time.Duration", ORDER + "Settings", Optional.empty(),
                        Optional.empty()));
        assertThat(details("web").properties()).containsExactly(new ModuleProperty("host", "java.lang.String",
                WEB + "WebConfig", Optional.empty(), Optional.empty()));
    }

    @Test
    void listsNamedInterfaces() {
        // arrange
        ModuleDetails catalog = details("catalog");

        // act
        Map<String, List<String>> interfaces = catalog.namedInterfaces();

        // assert
        assertThat(interfaces).containsOnly(Map.entry("<<UNNAMED>>", List.of(CATALOG + "Catalog")),
                Map.entry("spi", List.of(CATALOG + "spi.Pricing")));
        assertThat(details("shared").namedInterfaces())
                .containsOnly(Map.entry("<<UNNAMED>>", List.of(BASE + ".shared.internal.Util")));
        assertThat(details("order").namedInterfaces().get("<<UNNAMED>>")).contains(ORDER + "Holder")
                .doesNotContain(ORDER + "Holder$Inner", ORDER + "Hidden");
    }

    @ParameterizedTest
    @CsvSource({ "maxSize, max-size", "region, region", "URLPath, url-path", "httpURL, http-url", "a1B, a1-b" })
    void derivesKebabCaseNames(String method, String property) {
        // arrange
        String name = method;

        // act
        String kebab = ConfigPropertyCollector.kebab(name);

        // assert
        assertThat(kebab).isEqualTo(property);
    }

    static ModuleDetails details(String module) {
        return model.details(model.modules().getModuleByName(module).orElseThrow());
    }

    static ModuleBean bean(ModuleDetails details, String simpleName) {
        return details.beans().stream().filter(it -> it.type().name().endsWith("." + simpleName)).findFirst()
                .orElseThrow();
    }
}
