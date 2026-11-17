package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.core.fixtures.shop.Root;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;
import io.quarkiverse.modulith.core.fixtures.shop.catalog.api.Products;
import io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock;
import io.quarkiverse.modulith.core.fixtures.shop.inventory.InventoryListener;
import io.quarkiverse.modulith.core.fixtures.shop.order.Order;
import io.quarkiverse.modulith.core.fixtures.shop.order.OrderManagement;
import io.quarkiverse.modulith.core.fixtures.shop.order.OrderPlaced;

/**
 * The model of the {@code fixtures.shop} application, built from the test classes with Quarkus Modulith's annotations
 * and CDI, Jakarta, Panache and MicroProfile types.
 */
class ShopModelTest {

    static final String BASE = "io.quarkiverse.modulith.core.fixtures.shop";
    static final ApplicationModules MODULES = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

    static ApplicationModule module(String name) {
        return MODULES.getModuleByName(name).orElseThrow();
    }

    @Test
    void readsTheApplicationDeclaration() {
        // act
        String systemName = MODULES.getSystemName();

        // assert
        assertThat(systemName).isEqualTo("Shop");
        assertThat(MODULES.getBasePackage()).isEqualTo(BASE);
        assertThat(MODULES.getSource()).isEqualTo(ShopApplication.class.getName());
        assertThat(MODULES.getSharedModules()).extracting(ApplicationModule::getName).containsExactly("kernel");
    }

    @Test
    void describesTheMetadata() {
        // act
        ModulithMetadata metadata = MODULES.getMetadata();

        // assert
        assertThat(metadata.getSource()).isEqualTo(ShopApplication.class.getName());
        assertThat(metadata.getSystemName()).hasValue("Shop");
        assertThat(metadata.getBasePackages()).containsExactly(BASE);
        assertThat(metadata.getSharedModuleIdentifiers()).containsExactly(ApplicationModuleIdentifier.of("kernel"));
        assertThat(metadata.useFullyQualifiedModuleNames()).isFalse();
        assertThat(metadata).hasToString("ModulithMetadata[source=" + ShopApplication.class.getName() + ", basePackages=["
                + BASE + "]]");
    }

    @Test
    void readsTheMetadataOfAType() {
        // act
        ModulithMetadata metadata = ModulithMetadata.of(ShopApplication.class);

        // assert
        assertThat(metadata.getSystemName()).hasValue("Shop");
    }

    @Test
    void rendersTheDiagram() {
        // act
        String mermaid = MODULES.toMermaid();

        // assert
        assertThat(mermaid).startsWith("flowchart LR\n    catalog[\"Catalog\"]\n");
    }

    @Test
    void ordersTheModulesByDependency() {
        // act
        List<String> names = MODULES.stream().map(ApplicationModule::getName).toList();

        // assert
        assertThat(names).containsExactly("catalog", "kernel", "legacy", "catalog.pricing", "order", "inventory");
        assertThat(MODULES).extracting(ApplicationModule::getName).containsExactlyElementsOf(names);
    }

    @Test
    void identifiesTheModulesInOrder() {
        // act
        ApplicationModuleIdentifiers identifiers = ApplicationModuleIdentifiers.of(MODULES);

        // assert
        assertThat(identifiers).extracting(ApplicationModuleIdentifier::toString).containsExactly("catalog", "kernel",
                "legacy", "catalog.pricing", "order", "inventory");
    }

    @Test
    void identifiesListedModules() {
        // act
        ApplicationModuleIdentifiers identifiers = ApplicationModuleIdentifiers.of(List.of(ApplicationModuleIdentifier
                .of("x")));

        // assert
        assertThat(identifiers.stream()).containsExactly(ApplicationModuleIdentifier.of("x"));
    }

    @ParameterizedTest
    @CsvSource({ "inventory,order,1", "order,inventory,-1", "order,order,0", "catalog,kernel,-1" })
    void comparesModulesByDependencyOrder(String left, String right, int sign) {
        // act
        int result = MODULES.getComparator().compare(module(left), module(right));

        // assert
        assertThat(Integer.signum(result)).isEqualTo(sign);
    }

    @Test
    void comparesObjectsByTheOrderOfTheirModules() {
        // act
        int result = MODULES.getObjectComparator().compare(new InventoryListener(), Order.class);

        // assert
        assertThat(result).isPositive();
    }

    @Test
    void ordersObjectsOutsideModulesLast() {
        // act
        int result = MODULES.getObjectComparator().compare(Root.class, Order.class);

        // assert
        assertThat(result).isPositive();
    }

    @ParameterizedTest
    @CsvSource({ "catalog,Catalog,false,", "catalog.pricing,Pricing,false,catalog", "kernel,Shared kernel,true,",
            "inventory,Stock keeping,false,", "order,Order,false," })
    void readsTheModules(String name, String displayName, boolean open, String parent) {
        // act
        ApplicationModule module = module(name);

        // assert
        assertThat(module.getDisplayName()).isEqualTo(displayName);
        assertThat(module.isOpen()).isEqualTo(open);
        assertThat(MODULES.getParentOf(module).map(ApplicationModule::getName)).isEqualTo(Optional.ofNullable(parent));
        assertThat(MODULES.hasParent(module)).isEqualTo(parent != null);
        assertThat(module.isRootModule()).isFalse();
        assertThat(module.getIdentifier()).isEqualTo(ApplicationModuleIdentifier.of(name));
        assertThat(module.getJavaPackage().getName()).isEqualTo(module.getBasePackage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "catalog;catalog.pricing", "catalog.pricing;", "order;" })
    void nestsModules(String name, String nested) {
        // act
        List<ApplicationModule> result = MODULES.getNestedModules(module(name));

        // assert
        assertThat(result).extracting(ApplicationModule::getName)
                .isEqualTo(nested == null ? List.of() : List.of(nested));
    }

    @Test
    void leavesTheTypesOfNestedModulesOut() {
        // act
        boolean contains = module("catalog").contains(BASE + ".catalog.pricing.PriceList");

        // assert
        assertThat(contains).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "catalog,io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,true",
            "catalog,io.quarkiverse.modulith.core.ShopModelTest,false",
            "catalog.pricing,io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,false" })
    void decidesWhetherAModuleCouldContainAClass(String name, Class<?> type, boolean possible) {
        // act
        boolean result = module(name).couldContain(type);

        // assert
        assertThat(result).isEqualTo(possible);
    }

    @Test
    void findsTheRootModuleOfARootType() {
        // act
        Optional<ApplicationModule> root = MODULES.getModuleByType(Root.class);

        // assert
        assertThat(root.map(ApplicationModule::getName)).hasValue("root:" + BASE);
        assertThat(root.map(ApplicationModule::isRootModule)).hasValue(true);
        assertThat(MODULES.getModuleForType(Root.class)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "Stock,catalog", "io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,catalog",
            "io.quarkiverse.modulith.core.fixtures.shop.order.Order,order" })
    void findsTheModuleOfATypeName(String type, String module) {
        // act
        Optional<ApplicationModule> result = MODULES.getModuleByType(type);

        // assert
        assertThat(result.map(ApplicationModule::getName)).hasValue(module);
    }

    @Test
    void findsTheModuleOfAClass() {
        // act
        Optional<ApplicationModule> result = MODULES.getModuleByType(Stock.class);

        // assert
        assertThat(result.map(ApplicationModule::getName)).hasValue("catalog");
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.core.fixtures.shop.catalog.pricing,catalog.pricing",
            "io.quarkiverse.modulith.core.fixtures.shop.catalog.internal,catalog",
            "io.quarkiverse.modulith.core.fixtures.shop,root:io.quarkiverse.modulith.core.fixtures.shop", "com.acme," })
    void findsTheModuleOfAPackage(String packageName, String module) {
        // act
        Optional<ApplicationModule> result = MODULES.getModuleForPackage(packageName);

        // assert
        assertThat(result.map(ApplicationModule::getName)).isEqualTo(Optional.ofNullable(module));
    }

    @Test
    void findsNoModuleOfAnUnknownName() {
        // act
        Optional<ApplicationModule> result = MODULES.getModuleByName("nope");

        // assert
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,true",
            "io.quarkiverse.modulith.core.fixtures.shop.Root,false" })
    void knowsWhetherAClassBelongsToAModule(Class<?> type, boolean contained) {
        // act
        boolean result = MODULES.contains(type);

        // assert
        assertThat(result).isEqualTo(contained);
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.core.fixtures.shop.Root,true",
            "io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,false" })
    void knowsWhetherATypeLivesInARootPackage(String type, boolean root) {
        // act
        boolean result = MODULES.withinRootPackages(type);

        // assert
        assertThat(result).isEqualTo(root);
    }

    @Test
    void listsTheRootPackages() {
        // act
        List<JavaPackage> roots = MODULES.getRootPackages();

        // assert
        assertThat(roots).extracting(JavaPackage::getName).containsExactly(BASE);
    }

    @Test
    void reportsTheModulesOfDirectDependencies() {
        // act
        ApplicationModuleDependencies dependencies = module("order").getDirectDependencies(MODULES);

        // assert
        assertThat(dependencies.uniqueModules()).extracting(ApplicationModule::getName).containsExactly("catalog", "kernel");
        assertThat(dependencies.uniqueStream(ApplicationModuleDependency::getTargetModule))
                .extracting(it -> it.getTargetModule().getName()).containsExactly("catalog", "kernel");
        assertThat(dependencies.isEmpty()).isFalse();
    }

    @Test
    void reportsTypedDependencies() {
        // act
        ApplicationModuleDependencies components = module("order").getDirectDependencies(MODULES)
                .withType(DependencyType.USES_COMPONENT);

        // assert
        assertThat(components.stream())
                .extracting(ApplicationModuleDependency::getSourceType, ApplicationModuleDependency::getTargetType)
                .containsExactly(tuple(OrderManagement.class.getName(), Products.class.getName()));
    }

    @ParameterizedTest
    @CsvSource({ "kernel,true", "catalog,true", "inventory,false" })
    void knowsTheModulesOfDependencies(String name, boolean contained) {
        // act
        boolean result = module("order").getDirectDependencies(MODULES).containsModuleNamed(name);

        // assert
        assertThat(result).isEqualTo(contained);
        assertThat(module("order").getDirectDependencies(MODULES).contains(module(name))).isEqualTo(contained);
    }

    @ParameterizedTest
    @CsvSource({ "Products,true", "Nope,false" })
    void knowsTheTypesOfDependencies(String type, boolean contained) {
        // act
        boolean result = module("order").getDirectDependencies(MODULES).contains(type);

        // assert
        assertThat(result).isEqualTo(contained);
    }

    @Test
    void findsTheModuleOfADependencyType() {
        // act
        Optional<ApplicationModule> module = module("order").getDirectDependencies(MODULES).getModuleByType("Money");

        // assert
        assertThat(module.map(ApplicationModule::getName)).hasValue("kernel");
    }

    @Test
    void filtersDependenciesByType() {
        // act
        ApplicationModuleDependencies components = module("order").getDirectDependencies(MODULES,
                DependencyType.USES_COMPONENT);

        // assert
        assertThat(components.stream()).extracting(ApplicationModuleDependency::getTargetType)
                .containsExactly(Products.class.getName());
        assertThat(module("order").getDependencies(MODULES, DependencyType.ENTITY).isEmpty()).isTrue();
    }

    @Test
    void reportsNoDependenciesAtDepthNone() {
        // act
        ApplicationModuleDependencies none = module("order").getDependencies(MODULES, DependencyDepth.NONE);

        // assert
        assertThat(none.isEmpty()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "kernel,true", "legacy,false" })
    void knowsTheDeclaredDependencies(String name, boolean dependsOn) {
        // act
        boolean result = module("order").dependsOn(name);

        // assert
        assertThat(result).isEqualTo(dependsOn);
        assertThat(module("order").getDependencies()).containsExactly("catalog", "kernel");
    }

    @Test
    void followsDependenciesTransitively() {
        // act
        ApplicationModuleDependencies all = module("inventory").getAllDependencies(MODULES);

        // assert
        assertThat(all.uniqueModules()).extracting(ApplicationModule::getName).containsExactly("order", "catalog",
                "kernel");
    }

    @Test
    void followsDependenciesOfATypeTransitively() {
        // act
        ApplicationModuleDependencies listeners = module("inventory").getAllDependencies(MODULES,
                DependencyType.EVENT_LISTENER);

        // assert
        assertThat(listeners.uniqueModules()).extracting(ApplicationModule::getName).containsExactly("order");
    }

    @Test
    void listsTheEventsListenedTo() {
        // act
        List<JavaType> events = module("inventory").getEventsListenedTo(MODULES);

        // assert
        assertThat(events).extracting(JavaType::getName).containsExactly(OrderPlaced.class.getName());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "IMMEDIATE;order", "ALL;order,catalog", "NONE;" })
    void listsTheBootstrapDependencies(DependencyDepth depth, String names) {
        // act
        List<ApplicationModule> dependencies = module("inventory").getBootstrapDependencies(MODULES, depth);

        // assert
        assertThat(dependencies).extracting(ApplicationModule::getName)
                .isEqualTo(names == null ? List.of() : List.of(names.split(",")));
    }

    @Test
    void listsTheDirectBootstrapDependencies() {
        // act
        List<ApplicationModule> dependencies = module("inventory").getBootstrapDependencies(MODULES);

        // assert
        assertThat(dependencies).extracting(ApplicationModule::getName).containsExactly("order");
    }

    @Test
    void listsTheBootstrapBasePackages() {
        // act
        List<JavaPackage> packages = module("inventory").getBootstrapBasePackages(MODULES, DependencyDepth.IMMEDIATE)
                .toList();

        // assert
        assertThat(packages).extracting(JavaPackage::getName).containsExactly(BASE + ".inventory", BASE + ".order");
    }

    @Test
    @SuppressWarnings("deprecation")
    void listsTheBeans() {
        // act
        List<ModuleBean> beans = module("inventory").getBeans();

        // assert
        assertThat(beans).extracting(ModuleBean::getFullyQualifiedTypeName).containsExactly(
                BASE + ".inventory.InventoryConfig", BASE + ".inventory.InventoryInitializer",
                BASE + ".inventory.InventoryListener", BASE + ".inventory.StockRecords", BASE + ".inventory.StockValidator");
        assertThat(module("inventory").getSpringBeans()).isEqualTo(beans);
    }

    @Test
    @SuppressWarnings("deprecation")
    void listsTheBeansOfAType() {
        // act
        List<ModuleBean> initializers = module("inventory").getBeans(ApplicationModuleInitializer.class);

        // assert
        assertThat(initializers).extracting(ModuleBean::toString)
                .containsExactly(BASE + ".inventory.InventoryInitializer");
        assertThat(module("inventory").getSpringBeans(ApplicationModuleInitializer.class)).isEqualTo(initializers);
    }

    @Test
    void tellsApiBeans() {
        // act
        List<Boolean> api = module("catalog").getBeans().stream().map(ModuleBean::isApiBean).toList();

        // assert
        assertThat(api).containsExactly(true, false);
    }

    @Test
    void describesTheStereotypesOfAType() {
        // act
        ArchitecturallyEvidentType listener = module("inventory").getArchitecturallyEvidentType(InventoryListener.class);

        // assert
        assertThat(listener.getType().getName()).isEqualTo(InventoryListener.class.getName());
        assertThat(listener).hasToString(InventoryListener.class.getName());
        assertThat(List.of(listener.isController(), listener.isEventListener(), listener.isInjectable(),
                listener.isEntity(), listener.isAggregateRoot(), listener.isRepository(), listener.isService(),
                listener.isConfigurationProperties(), listener.isValueObject()))
                .containsExactly(true, true, true, false, false, false, false, false, false);
    }

    @Test
    void describesTheListenerMethodsOfAType() {
        // act
        ArchitecturallyEvidentType listener = module("inventory").getArchitecturallyEvidentType(InventoryListener.class);

        // assert
        assertThat(listener.getReferenceMethods()).extracting(ReferenceMethod::getName, ReferenceMethod::isAsync)
                .containsExactly(tuple("observe", false), tuple("observeAsync", true), tuple("on", true));
    }

    @Test
    void recognizesConfigurationBeans() {
        // act
        ArchitecturallyEvidentType config = module("inventory").getBeans().get(0).toArchitecturallyEvidentType();

        // assert
        assertThat(config.isConfigurationProperties()).isTrue();
    }

    @Test
    void listsTheAggregateRootsAndValueTypes() {
        // act
        List<JavaType> roots = module("order").getAggregateRoots();

        // assert
        assertThat(roots).extracting(JavaType::getSimpleName).containsExactly("LineItem", "Order");
        assertThat(module("order").getValueTypes()).isEmpty();
    }

    @Test
    void rejectsStereotypesOfTypesOfOtherModules() {
        // arrange
        ApplicationModule order = module("order");

        // act & assert
        assertThatThrownBy(() -> order.getArchitecturallyEvidentType(Stock.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Couldn't find type " + Stock.class.getName() + " in module order!");
    }

    @Test
    void describesThePublishedEvents() {
        // act
        List<EventType> events = module("order").getPublishedEventTypes();

        // assert
        assertThat(events).singleElement().satisfies(it -> {
            assertThat(it.getType().getName()).isEqualTo(OrderPlaced.class.getName());
            assertThat(it.hasSources()).isTrue();
            assertThat(it.getSources()).extracting(source -> source.toString(module("order")))
                    .containsExactly("i.q.m.c.f.s.o.OrderManagement.complete(…)");
            assertThat(it).hasToString("EventType [type=" + OrderPlaced.class.getName() + ", sources=["
                    + OrderManagement.class.getName() + ".complete]]");
        });
        assertThat(module("order").getPublishedEvents()).containsExactly(OrderPlaced.class.getName());
    }

    @Test
    void listsTheListenedEvents() {
        // act
        List<String> events = module("inventory").getListenedEvents();

        // assert
        assertThat(events).containsExactly(OrderPlaced.class.getName());
    }

    @Test
    void describesHttpEntryPoints() {
        // act
        List<EntryPoint> entryPoints = module("order").getEntryPoints();

        // assert
        assertThat(entryPoints).containsExactly(
                new EntryPoint(BASE + ".order.OrderResource", "get", EntryPointKind.HTTP, "GET /orders/{id}"),
                new EntryPoint(BASE + ".order.OrderResource", "create", EntryPointKind.HTTP, "POST /orders"),
                new EntryPoint(BASE + ".order.OrderResource", "items", EntryPointKind.HTTP, "* /orders/items"));
    }

    @Test
    void describesEntryPointsOfEveryKind() {
        // act
        List<EntryPoint> entryPoints = module("inventory").getEntryPoints();

        // assert
        assertThat(entryPoints).extracting(EntryPoint::kind, EntryPoint::detail).containsExactly(
                tuple(EntryPointKind.EVENT_LISTENER, OrderPlaced.class.getName()),
                tuple(EntryPointKind.EVENT_LISTENER, OrderPlaced.class.getName()),
                tuple(EntryPointKind.EVENT_LISTENER, OrderPlaced.class.getName()),
                tuple(EntryPointKind.MESSAGING, "value=orders"),
                tuple(EntryPointKind.SCHEDULED, "every=10s"),
                tuple(EntryPointKind.HTTP, "path=/stock"));
    }

    @Test
    void exposesNamedInterfaces() {
        // act
        NamedInterfaces interfaces = module("catalog").getNamedInterfaceDetails();

        // assert
        assertThat(module("catalog").getNamedInterfaces()).containsExactlyInAnyOrder("api", "spi");
        assertThat(interfaces.getByName("spi").map(NamedInterface::getPackages)).hasValue(List.of());
        assertThat(interfaces.getByName("api").map(NamedInterface::getPackages)).hasValue(List.of(BASE + ".catalog.api"));
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.core.fixtures.shop.catalog.api.Products,true",
            "io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,false" })
    void knowsTheExposedTypes(Class<?> type, boolean exposed) {
        // act
        boolean result = module("catalog").isExposed(type);

        // assert
        assertThat(result).isEqualTo(exposed);
    }

    @Test
    void listsTheInternalTypes() {
        // act
        List<JavaType> internal = module("catalog").getInternalTypes();

        // assert
        assertThat(internal).extracting(JavaType::getSimpleName).containsExactly("package-info", "Stock");
    }

    @ParameterizedTest
    @CsvSource({ "Products,true", "io.quarkiverse.modulith.core.fixtures.shop.catalog.api.Products,true",
            "com.acme.Products,false", "Nope,false" })
    void knowsTheTypesOfTheModule(String type, boolean contained) {
        // act
        boolean result = module("catalog").contains(type);

        // assert
        assertThat(result).isEqualTo(contained);
    }

    @Test
    void knowsTheClassesOfTheModule() {
        // act
        boolean contained = module("catalog").contains(Products.class);

        // assert
        assertThat(contained).isTrue();
        assertThat(module("catalog").contains(Order.class)).isFalse();
    }

    @Test
    void parsesAllowedDependencies() {
        // act
        AllowedDependencies allowed = module("order").getAllowedDependencies(MODULES);

        // assert
        assertThat(allowed).hasToString("kernel, catalog :: api");
        assertThat(allowed.isClosed()).isTrue();
        assertThat(allowed.isEmpty()).isFalse();
        assertThat(allowed.stream()).extracting(it -> it.getTargetModule().getName()).containsExactly("kernel",
                "catalog");
        assertThat(allowed.stream().skip(1).findFirst().flatMap(AllowedDependency::getTargetNamedInterface)
                .map(NamedInterface::getName)).hasValue("api");
    }

    @Test
    void comparesAllowedDependencies() {
        // act
        AllowedDependencies allowed = module("order").getAllowedDependencies(MODULES);

        // assert
        assertThat(allowed).isEqualTo(module("order").getAllowedDependencies(MODULES))
                .hasSameHashCodeAs(module("order").getAllowedDependencies(MODULES)).isNotEqualTo("kernel");
    }

    @Test
    void keepsTheDeclaredAllowedDependencies() {
        // act
        Optional<List<String>> declared = module("order").getAllowedDependencies();

        // assert
        assertThat(declared).hasValue(List.of("kernel", "catalog :: api"));
    }

    @Test
    void leavesUnrestrictedModulesOpen() {
        // act
        AllowedDependencies allowed = module("inventory").getAllowedDependencies(MODULES);

        // assert
        assertThat(allowed).hasToString("none");
        assertThat(allowed.isClosed()).isFalse();
        assertThat(module("inventory").getAllowedDependencies()).isEmpty();
    }

    @Test
    void detectsNoViolations() {
        // act
        Violations violations = MODULES.detectViolations();

        // assert
        assertThat(violations.hasViolations()).isFalse();
    }

    @Test
    void verifiesTheModel() {
        // act
        ApplicationModules verified = MODULES.verify();

        // assert
        assertThat(verified).isSameAs(MODULES);
        assertThat(MODULES.verify(VerificationOptions.defaults())).isSameAs(MODULES);
    }

    @Test
    void verifiesTheDependenciesOfAModule() {
        // arrange
        ApplicationModule order = module("order");

        // act
        Violations violations = order.detectDependencies(MODULES);

        // assert
        assertThat(violations.hasViolations()).isFalse();
        assertThatCode(() -> order.verifyDependencies(MODULES)).doesNotThrowAnyException();
    }

    @Test
    void describesItself() {
        // act
        String description = module("order").toString(MODULES);

        // assert
        assertThat(description).isEqualTo("""
                # Order
                > Logical name: order
                > Base package: %1$s.order
                > Excluded packages: none
                > Direct module dependencies:\s
                  + catalog
                  - kernel
                > Beans:
                  + ….OrderManagement
                  + ….OrderRepository
                  + ….OrderResource
                """.formatted(BASE));
        assertThat(module("kernel")).hasToString("""
                # Shared kernel (open)
                > Logical name: kernel
                > Base package: %1$s.kernel
                > Excluded packages: none
                > Beans: none
                """.formatted(BASE));
        assertThat(MODULES.toString()).startsWith("# Catalog\n").contains("\n# Shared kernel (open)\n");
        assertThat(module("order")).isEqualTo(module("order")).hasSameHashCodeAs(module("order"))
                .isNotEqualTo(module("kernel")).isNotEqualTo("order");
    }
}
