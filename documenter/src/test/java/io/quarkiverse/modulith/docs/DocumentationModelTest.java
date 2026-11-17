package io.quarkiverse.modulith.docs;

import static io.quarkiverse.modulith.docs.TestModels.module;
import static io.quarkiverse.modulith.docs.TestModels.modules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.fixtures.shop.ShopApplication;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

class DocumentationModelTest {

    static final String SHOP = "io.quarkiverse.modulith.docs.fixtures.shop.";

    static final ApplicationModules MODULES = modules("Shop",
            module("order", List.of("catalog"), List.of("com.acme.order.OrderCompleted", "com.acme.order.internal.Hidden"),
                    List.of()),
            module("inventory", List.of("order"), List.of(),
                    List.of("com.acme.order.OrderCompleted", "com.acme.catalog.Elsewhere")),
            module("catalog", List.of()));

    @Test
    void derivesDetailsFromTheCoreModuleModel() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

        // act
        DocumentationModel model = DocumentationModel.of(modules);

        // assert
        ModuleDetails inventory = model.details(modules.getModuleByName("inventory").orElseThrow());
        ModuleDetails order = model.details(modules.getModuleByName("order").orElseThrow());
        assertThat(inventory.dependencies()).first().isEqualTo(new ModuleDependency(SHOP + "inventory.InventoryManagement",
                SHOP + "order.OrderCancelled", DependencyType.EVENT_LISTENER, "order"));
        assertThat(inventory.beans()).extracting(it -> it.type().name(), ModuleBean::stereotypes).first()
                .isEqualTo(tuple(SHOP + "inventory.InventoryManagement", Set.of(Stereotype.EVENT_LISTENER)));
        assertThat(order.publishedEvents()).extracting(it -> it.type().name(), PublishedEvent::sources)
                .containsExactly(
                        tuple(SHOP + "order.OrderCancelled",
                                List.of(new EventSource("i.q.m.d.f.s.o.OrderManagement", "cancel", false))),
                        tuple(SHOP + "order.OrderCompleted",
                                List.of(new EventSource("i.q.m.d.f.s.o.OrderManagement", "complete", true))));
        assertThat(order.beans()).extracting(ModuleBean::stereotypes).containsExactly(Set.of(Stereotype.CONTROLLER),
                Set.of(Stereotype.SERVICE), Set.of(Stereotype.CONFIGURATION_PROPERTIES), Set.of(Stereotype.REPOSITORY));
        assertThat(order.aggregateRoots()).extracting(ModuleType::name).containsExactly(SHOP + "order.Order");
        assertThat(order.valueTypes()).extracting(ModuleType::name).containsExactly(SHOP + "order.LineItem");
        assertThat(order.properties()).isEmpty();
        assertThat(model.sharedModules()).isEmpty();
        assertThat(model.modules()).isSameAs(modules);
    }

    @ParameterizedTest
    @CsvSource(value = { "Shop, Shop", "'', Modules", "'  ', Modules", "NULL, Modules" }, nullValues = "NULL")
    void defaultsTheSystemName(String systemName, String expected) {
        // arrange
        ApplicationModules modules = modules(systemName);

        // act
        String name = new DocumentationModel(modules, List.of(), List.of(), DocumentationSource.none()).systemName();

        // assert
        assertThat(name).isEqualTo(expected);
    }

    @Test
    void findsTypesByBinaryOrSimpleName() {
        // arrange
        ModuleType order = ModuleType.of("com.acme.order.Order$Line");
        ModuleType catalog = ModuleType.of("com.acme.catalog.Line");
        DocumentationModel model = new DocumentationModel(MODULES,
                List.of(details("order", List.of(order)), details("catalog", List.of(catalog))), List.of(),
                DocumentationSource.none());

        // act
        Optional<ModuleType> bySimpleName = model.type("Line");
        Optional<ModuleType> byBinaryName = model.type("com.acme.catalog.Line");
        Optional<ModuleType> unknownSimple = model.type("Other");
        Optional<ModuleType> outside = model.type("org.other.Line");

        // assert
        assertThat(bySimpleName).contains(catalog);
        assertThat(byBinaryName).contains(catalog);
        assertThat(unknownSimple).isEmpty();
        assertThat(outside).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "com.acme.order.Exposed, true", "com.acme.order.internal.Hidden, false",
            "com.acme.order.BasePackage, true", "com.acme.order.internal.Unknown, false", "org.other.Type, true" })
    void tellsWhetherATypeIsExposed(String type, boolean exposed) {
        // arrange
        DocumentationModel model = new DocumentationModel(MODULES, List.of(details("order",
                List.of(ModuleType.of("com.acme.order.Exposed"), new ModuleType("com.acme.order.internal.Hidden",
                        true, false, Set.of(), Set.of(), Set.of(), Set.of())))),
                List.of(), DocumentationSource.none());

        // act
        boolean actual = model.isExposed(type);

        // assert
        assertThat(actual).isEqualTo(exposed);
    }

    @Test
    void navigatesNestedAndSharedModules() {
        // arrange
        ModuleDetails nested = new ModuleDetails("inventory", Optional.of("order"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        DocumentationModel model = new DocumentationModel(MODULES, List.of(nested), List.of("catalog", "unknown"),
                DocumentationSource.none());
        ApplicationModule order = MODULES.getModuleByName("order").orElseThrow();
        ApplicationModule inventory = MODULES.getModuleByName("inventory").orElseThrow();

        // act
        Optional<ApplicationModule> parent = model.parentOf(inventory);
        List<ApplicationModule> children = model.nestedModules(order);

        // assert
        assertThat(parent).contains(order);
        assertThat(model.hasParent(inventory)).isTrue();
        assertThat(model.hasParent(order)).isFalse();
        assertThat(children).containsExactly(inventory);
        assertThat(model.nestedModules(inventory)).isEmpty();
        assertThat(model.sharedModules()).extracting(ApplicationModule::getName).containsExactly("catalog");
    }

    @Test
    void listsDependenciesByType() {
        // arrange
        DocumentationModel model = chain();
        ApplicationModule order = MODULES.getModuleByName("order").orElseThrow();

        // act
        List<ApplicationModule> all = model.directDependencies(order);
        List<ApplicationModule> listeners = model.directDependencies(order, DependencyType.EVENT_LISTENER);

        // assert
        assertThat(all).extracting(ApplicationModule::getName).containsExactly("inventory", "catalog");
        assertThat(listeners).extracting(ApplicationModule::getName).containsExactly("catalog");
    }

    @ParameterizedTest
    @EnumSource(DependencyDepth.class)
    void followsBootstrapDependencies(DependencyDepth depth) {
        // arrange
        DocumentationModel model = chain();
        ApplicationModule order = MODULES.getModuleByName("order").orElseThrow();

        // act
        List<ApplicationModule> dependencies = model.bootstrapDependencies(order, depth);

        // assert
        List<String> expected = switch (depth) {
            case NONE -> List.of();
            case IMMEDIATE -> List.of("inventory");
            case ALL -> List.of("inventory", "catalog", "order");
        };
        assertThat(dependencies).extracting(ApplicationModule::getName).containsExactlyElementsOf(expected);
    }

    @Test
    void groupsDependencyTypesByTargetWithoutRedundantDefaults() {
        // arrange
        DocumentationModel model = chain();
        ApplicationModule order = MODULES.getModuleByName("order").orElseThrow();

        // act
        Map<ApplicationModule, Set<DependencyType>> types = model.dependencyTypesByModule(order);

        // assert
        assertThat(types).containsExactly(
                Map.entry(MODULES.getModuleByName("inventory").orElseThrow(), Set.of(DependencyType.USES_COMPONENT)),
                Map.entry(MODULES.getModuleByName("catalog").orElseThrow(), Set.of(DependencyType.EVENT_LISTENER)));
    }

    @Test
    void usesEmptyDetailsForUnknownModules() {
        // arrange
        DocumentationModel model = new DocumentationModel(MODULES, List.of(), List.of(), DocumentationSource.none());
        ApplicationModule catalog = MODULES.getModuleByName("catalog").orElseThrow();

        // act
        ModuleDetails details = model.details(catalog);

        // assert
        assertThat(details).isEqualTo(ModuleDetails.of("catalog"));
        assertThat(model.documentation()).isSameAs(DocumentationSource.none());
    }

    @Test
    void keepsTheNamedInterfacesInDeclarationOrder() {
        // arrange
        Map<String, List<String>> declared = new LinkedHashMap<>();
        List.of("zeta", "eta", "alpha", "theta", "beta", "kappa", "gamma", "iota", "delta", "epsilon")
                .forEach(name -> declared.put(name, List.of("com.acme.order." + name + ".Api")));

        // act
        ModuleDetails details = new ModuleDetails("order", Optional.empty(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), declared, List.of());
        Throwable modified = catchThrowable(() -> details.namedInterfaces().clear());

        // assert
        assertThat(details.namedInterfaces()).containsExactlyEntriesOf(declared);
        assertThat(modified).isInstanceOf(UnsupportedOperationException.class);
    }

    static DocumentationModel chain() {
        ModuleDetails order = new ModuleDetails("order", Optional.empty(), List.of(), List.of(), List.of(
                new ModuleDependency("com.acme.order.A", "com.acme.inventory.B", DependencyType.USES_COMPONENT,
                        "inventory"),
                new ModuleDependency("com.acme.order.A", "com.acme.inventory.B", DependencyType.USES_COMPONENT,
                        "inventory"),
                new ModuleDependency("com.acme.order.A", "com.acme.catalog.C", DependencyType.DEFAULT, "catalog"),
                new ModuleDependency("com.acme.order.A", "com.acme.catalog.E", DependencyType.EVENT_LISTENER,
                        "catalog"),
                new ModuleDependency("com.acme.order.A", "org.Gone", DependencyType.DEFAULT, "gone")),
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        ModuleDetails inventory = new ModuleDetails("inventory", Optional.empty(), List.of(), List.of(), List.of(
                new ModuleDependency("com.acme.inventory.B", "com.acme.catalog.C", DependencyType.USES_COMPONENT,
                        "catalog")),
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        ModuleDetails catalog = new ModuleDetails("catalog", Optional.empty(), List.of(), List.of(), List.of(
                new ModuleDependency("com.acme.catalog.C", "com.acme.order.A", DependencyType.USES_COMPONENT,
                        "order")),
                List.of(), List.of(), List.of(), List.of(), Map.of(), List.of());
        return new DocumentationModel(MODULES, List.of(order, inventory, catalog), List.of(),
                DocumentationSource.none());
    }

    static ModuleDetails details(String identifier, List<ModuleType> types) {
        return new ModuleDetails(identifier, Optional.empty(), types, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), Map.of(), List.of());
    }
}
