package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.fixtures.app.audit.AuditLogged;
import io.quarkiverse.modulith.events.fixtures.app.audit.AuditUntargeted;
import io.quarkiverse.modulith.events.fixtures.app.audit.LoginAudited;
import io.quarkiverse.modulith.events.fixtures.app.audit.Topic;
import io.quarkiverse.modulith.events.fixtures.app.billing.CustomerCreated;
import io.quarkiverse.modulith.events.fixtures.app.billing.DoubleBilled;
import io.quarkiverse.modulith.events.fixtures.app.billing.InvoiceIssued;
import io.quarkiverse.modulith.events.fixtures.app.billing.InvoicePaid;
import io.quarkiverse.modulith.events.fixtures.app.billing.RefundIssued;
import io.quarkiverse.modulith.events.fixtures.app.inventory.BlankJMolecules;
import io.quarkiverse.modulith.events.fixtures.app.inventory.BothAnnotations;
import io.quarkiverse.modulith.events.fixtures.app.inventory.StockChanged;
import io.quarkiverse.modulith.events.fixtures.app.order.KeyOnly;
import io.quarkiverse.modulith.events.fixtures.app.order.OrderPlaced;
import io.quarkiverse.modulith.events.fixtures.app.order.OrderShipped;
import io.quarkiverse.modulith.events.fixtures.app.order.TargetAlias;
import io.quarkiverse.modulith.events.fixtures.appendix.Appendix;
import io.quarkiverse.modulith.events.fixtures.other.Outside;

class EventExternalizationConfigurationTest {

    private static final String APP = "io.quarkiverse.modulith.events.fixtures.app";

    static Stream<Arguments> defaultSelection() {
        return Stream.of(
                Arguments.of(new OrderPlaced("o-1"), true),
                Arguments.of(new InvoicePaid("i-1"), true),
                Arguments.of(new CustomerCreated("Doe"), true),
                Arguments.of(new StockChanged("s-1"), true),
                Arguments.of(new RefundIssued(), true),
                Arguments.of(new InvoiceIssued("i-2"), false),
                Arguments.of(new Outside("x"), false),
                Arguments.of(new Appendix("x"), false));
    }

    @ParameterizedTest
    @MethodSource
    void defaultSelection(Object event, boolean selected) {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.defaults(APP).build();

        // act
        boolean supported = configuration.supports(event);

        // assert
        assertThat(supported).isEqualTo(selected);
    }

    static Stream<Arguments> defaultRouting() {
        return Stream.of(
                Arguments.of(new OrderPlaced("o-1"), "order.OrderPlaced"),
                Arguments.of(new OrderShipped("o-1"), "shipments::#{#this.orderId()}"),
                Arguments.of(new KeyOnly("k"), "order.KeyOnly::{event.id}"),
                Arguments.of(new TargetAlias("t"), "alias-target"),
                Arguments.of(new InvoicePaid("i-1"), "billing"),
                Arguments.of(new DoubleBilled(), "billing"),
                Arguments.of(new RefundIssued(), "RefundIssued::base-key"),
                Arguments.of(new CustomerCreated("Doe"), "domain"),
                Arguments.of(new StockChanged("s-1"), "stock"),
                Arguments.of(new BothAnnotations("s-1"), "jmolecules-target"),
                Arguments.of(new BlankJMolecules("s-1"), "inventory.BlankJMolecules"),
                Arguments.of(new InvoiceIssued("i-2"), "InvoiceIssued"),
                Arguments.of(new Outside("x"), Outside.class.getName()));
    }

    @ParameterizedTest
    @MethodSource
    void defaultRouting(Object event, String expected) {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration
                .defaults(List.of(APP + ".billing", APP)).build();

        // act
        RoutingTarget target = configuration.determineTarget(event);

        // assert
        assertThat(target).hasToString(expected);
    }

    @Test
    void defaultsSendTheEventAsIsWithoutHeadersAndConcurrently() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1");
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.defaults(APP).build();

        // act
        Object mapped = configuration.map(event);

        // assert
        assertThat(mapped).isSameAs(event);
        assertThat(configuration.getHeadersFor(event)).isEmpty();
        assertThat(configuration.serializeExternalization()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("missingPackages")
    void defaultsNeedPackages(List<String> packages) {
        // act & assert
        assertThatThrownBy(() -> EventExternalizationConfiguration.defaults(packages))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Packages must not be empty");
    }

    static Stream<List<String>> missingPackages() {
        return Stream.of(List.of(), null);
    }

    @Test
    void disabledSelectsNothing() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.disabled();

        // act
        boolean supported = configuration.supports(new OrderPlaced("o-1"));

        // assert
        assertThat(supported).isFalse();
    }

    static Stream<Arguments> selectors() {
        EventExternalizationConfiguration.Selector selector = EventExternalizationConfiguration.externalizing();
        return Stream.of(
                Arguments.of(selector.select(event -> event instanceof OrderPlaced), new OrderPlaced("o"),
                        new InvoiceIssued("i")),
                Arguments.of(selector.selectByPackage(APP + ".order"), new OrderPlaced("o"),
                        new InvoiceIssued("i")),
                Arguments.of(selector.selectByPackage(APP), new InvoiceIssued("i"), new Appendix("a")),
                Arguments.of(selector.selectByPackage(InvoiceIssued.class), new InvoicePaid("i"),
                        new OrderPlaced("o")),
                Arguments.of(selector.selectByPackagesAndFilter(List.of(APP + ".order", APP + ".billing"),
                        event -> !(event instanceof KeyOnly)), new InvoiceIssued("i"), new KeyOnly("k")),
                Arguments.of(selector.selectByPackagesAndFilter(List.of(APP + ".order"), event -> true),
                        new OrderPlaced("o"), new StockChanged("s")),
                Arguments.of(selector.selectByType(Record.class), new OrderPlaced("o"), new RefundIssued()),
                Arguments.of(selector.selectByType(type -> type.getSimpleName().startsWith("Order")),
                        new OrderShipped("o"), new KeyOnly("k")),
                Arguments.of(selector.selectByAnnotation(Topic.class), new LoginAudited("u"),
                        new OrderPlaced("o")),
                Arguments.of(selector.selectByAnnotation(Externalized.class), new CustomerCreated("c"),
                        new StockChanged("s")),
                Arguments.of(selector.annotatedAsExternalized(), new StockChanged("s"), new InvoiceIssued("i")));
    }

    @ParameterizedTest
    @MethodSource
    void selectors(EventExternalizationConfiguration.Router router, Object selected, Object ignored) {
        // arrange
        EventExternalizationConfiguration configuration = router.build();

        // act
        boolean selects = configuration.supports(selected);
        boolean ignores = !configuration.supports(ignored);

        // assert
        assertThat(selects).isTrue();
        assertThat(ignores).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidSelections")
    void selectorsRejectMissingArguments(Consumer<EventExternalizationConfiguration.Selector> selection,
            Class<? extends Throwable> type, String message) {
        // arrange
        EventExternalizationConfiguration.Selector selector = EventExternalizationConfiguration.externalizing();

        // act & assert
        assertThatThrownBy(() -> selection.accept(selector)).isInstanceOf(type).hasMessage(message);
    }

    static Stream<Arguments> invalidSelections() {
        return Stream.of(
                Arguments.of(selection(selector -> selector.selectByPackage(" ")), IllegalArgumentException.class,
                        "Base package must not be blank"),
                Arguments.of(selection(selector -> selector.selectByPackage((String) null)),
                        IllegalArgumentException.class, "Base package must not be blank"),
                Arguments.of(selection(selector -> selector.selectByPackagesAndFilter(List.of(), event -> true)),
                        IllegalArgumentException.class, "Base packages must not be empty"),
                Arguments.of(selection(selector -> selector.selectByPackagesAndFilter(null, event -> true)),
                        IllegalArgumentException.class, "Base packages must not be empty"),
                Arguments.of(selection(selector -> selector.select(null)), NullPointerException.class, "filter"));
    }

    static Consumer<EventExternalizationConfiguration.Selector> selection(
            Consumer<EventExternalizationConfiguration.Selector> selection) {
        return selection;
    }

    @Test
    void routesByAnnotationValue() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .selectAndRoute(Topic.class, Topic::value).build();

        // act
        RoutingTarget logged = configuration.determineTarget(new AuditLogged("a"));
        RoutingTarget untargeted = configuration.determineTarget(new AuditUntargeted("a"));
        RoutingTarget inherited = configuration.determineTarget(new LoginAudited("u"));

        // assert
        assertThat(logged).hasToString("audit::k-1");
        assertThat(untargeted).hasToString(AuditUntargeted.class.getName() + "::k-2");
        assertThat(inherited).hasToString("audited");
        assertThat(configuration.supports(new OrderPlaced("o"))).isFalse();
        assertThatThrownBy(() -> configuration.determineTarget(new OrderPlaced("o")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No annotation " + Topic.class.getName() + " on type " + OrderPlaced.class.getName());
    }

    @Test
    void routesByEventAndAnnotation() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .selectAndRoute(Topic.class,
                        (event, topic) -> RoutingTarget.forTarget(
                                ParsedRoutingTarget.parse(topic.value()).getTarget().orElseThrow() + "-"
                                        + ((AuditLogged) event).id()))
                .build();

        // act
        RoutingTarget target = configuration.determineTarget(new AuditLogged("a-7"));

        // assert
        assertThat(target).hasToString("audit-a-7");
    }

    @Test
    void defaultRouterUsesTheAnnotationOrTheTypeName() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .select(event -> true).build();

        // act
        RoutingTarget placed = configuration.determineTarget(new OrderPlaced("o"));
        RoutingTarget keyOnly = configuration.determineTarget(new KeyOnly("k"));
        RoutingTarget shipped = configuration.determineTarget(new OrderShipped("o"));

        // assert
        assertThat(placed).hasToString(OrderPlaced.class.getName());
        assertThat(keyOnly).hasToString(KeyOnly.class.getName() + "::{event.id}");
        assertThat(shipped).hasToString("shipments::#{#this.orderId()}");
    }

    @Test
    void mapsGloballyAndPerType() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .select(event -> true)
                .mapping(event -> "global:" + event)
                .mapping(OrderPlaced.class, order -> order.id())
                .build();

        // act
        Object order = configuration.map(new OrderPlaced("o-1"));
        Object other = configuration.map(new InvoiceIssued("i-1"));

        // assert
        assertThat(order).isEqualTo("global:o-1");
        assertThat(other).isEqualTo("global:InvoiceIssued[number=i-1]");
    }

    @Test
    void addsHeadersGloballyAndPerType() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .select(event -> true)
                .headers(event -> Map.of("kind", "event"))
                .headers(OrderPlaced.class, order -> Map.of("order", order.id()))
                .headers(InvoiceIssued.class, invoice -> null)
                .build();
        EventExternalizationConfiguration withoutHeaders = EventExternalizationConfiguration.externalizing()
                .select(event -> true)
                .headers(event -> null)
                .build();

        // act
        Map<String, Object> order = configuration.getHeadersFor(new OrderPlaced("o-1"));
        Map<String, Object> other = configuration.getHeadersFor(new KeyOnly("k"));
        Map<String, Object> typeMissing = configuration.getHeadersFor(new InvoiceIssued("i"));
        Map<String, Object> missing = withoutHeaders.getHeadersFor(new InvoiceIssued("i"));

        // assert
        assertThat(order).containsExactly(Map.entry("order", "o-1"));
        assertThat(other).containsExactly(Map.entry("kind", "event"));
        assertThat(typeMissing).containsExactly(Map.entry("kind", "event"));
        assertThat(missing).isEmpty();
    }

    @Test
    void routesPerTypeAndByKey() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .select(event -> true)
                .routeAll(event -> RoutingTarget.forTarget("all"))
                .route(OrderPlaced.class, order -> RoutingTarget.forTarget("orders"))
                .routeKey(OrderPlaced.class, OrderPlaced::id)
                .routeKey(InvoiceIssued.class, InvoiceIssued::number)
                .build();

        // act
        RoutingTarget order = configuration.determineTarget(new OrderPlaced("o-1"));
        RoutingTarget invoice = configuration.determineTarget(new InvoiceIssued("i-1"));
        RoutingTarget other = configuration.determineTarget(new KeyOnly("k"));

        // assert
        assertThat(order).hasToString("orders::o-1");
        assertThat(invoice).hasToString("all::i-1");
        assertThat(other).hasToString("all");
    }

    @Test
    void routesByTypeAndMappedPayload() {
        // arrange
        EventExternalizationConfiguration.Router router = EventExternalizationConfiguration.externalizing()
                .select(event -> true)
                .mapping(OrderPlaced.class, order -> new InvoiceIssued(order.id()));

        // act
        RoutingTarget byType = router.routeAllByType(type -> RoutingTarget.forTarget(type.getSimpleName())).build()
                .determineTarget(new OrderPlaced("o-1"));
        RoutingTarget mapped = router.routeAllByType(type -> RoutingTarget.forTarget(type.getSimpleName()))
                .routeMapped().build().determineTarget(new OrderPlaced("o-1"));
        RoutingTarget both = router.routeMapped((event, payload) -> RoutingTarget.forTarget(
                event.getClass().getSimpleName()).andKey(((InvoiceIssued) payload).number())).build()
                .determineTarget(new OrderPlaced("o-2"));

        // assert
        assertThat(byType).hasToString("OrderPlaced");
        assertThat(mapped).hasToString("InvoiceIssued");
        assertThat(both).hasToString("OrderPlaced::o-2");
    }

    @Test
    void routesOptionally() {
        // arrange
        EventExternalizationConfiguration.Router router = EventExternalizationConfiguration.externalizing()
                .select(event -> true)
                .routeAll(event -> RoutingTarget.forTarget("fallback"));
        EventExternalizationConfiguration byEvent = router.routeOptional(
                event -> Optional.of(event).filter(OrderPlaced.class::isInstance)
                        .map(order -> RoutingTarget.forTarget("orders")));
        EventExternalizationConfiguration byType = router.routeOptionalByType(
                type -> Optional.of(type).filter(InvoiceIssued.class::equals)
                        .map(invoice -> RoutingTarget.forTarget("invoices")));

        // act
        RoutingTarget routedEvent = byEvent.determineTarget(new OrderPlaced("o"));
        RoutingTarget unroutedEvent = byEvent.determineTarget(new KeyOnly("k"));
        RoutingTarget routedType = byType.determineTarget(new InvoiceIssued("i"));
        RoutingTarget unroutedType = byType.determineTarget(new KeyOnly("k"));

        // assert
        assertThat(routedEvent).hasToString("orders");
        assertThat(unroutedEvent).hasToString("fallback");
        assertThat(routedType).hasToString("invoices");
        assertThat(unroutedType).hasToString("fallback");
    }

    @Test
    void serializesOnRequest() {
        // arrange
        EventExternalizationConfiguration.Router router = EventExternalizationConfiguration.externalizing()
                .select(event -> true);

        // act
        boolean serialized = router.serializeExternalization().build().serializeExternalization();
        boolean reset = router.serializeExternalization().serializeExternalization(false).build()
                .serializeExternalization();

        // assert
        assertThat(serialized).isTrue();
        assertThat(reset).isFalse();
    }

    @Test
    void rejectsAMissingTarget() {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .select(event -> true).routeAll(event -> null).build();

        // act & assert
        assertThatThrownBy(() -> configuration.determineTarget(new OrderPlaced("o")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No routing target for an event of type " + OrderPlaced.class.getName()
                        + ". Make sure the externalization configuration always produces a target");
    }

    @ParameterizedTest
    @MethodSource("eventOperations")
    void rejectsAMissingEvent(Consumer<EventExternalizationConfiguration> operation) {
        // arrange
        EventExternalizationConfiguration configuration = EventExternalizationConfiguration.externalizing()
                .select(event -> true).routeAll(event -> RoutingTarget.forTarget("all")).build();

        // act & assert
        assertThatThrownBy(() -> operation.accept(configuration)).isInstanceOf(NullPointerException.class)
                .hasMessage("event");
    }

    static Stream<Consumer<EventExternalizationConfiguration>> eventOperations() {
        return Stream.of(configuration -> configuration.supports(null), configuration -> configuration.map(null),
                configuration -> configuration.determineTarget(null),
                configuration -> configuration.getHeadersFor(null));
    }

    @Test
    void exposesTheBuildingBlocks() {
        // arrange
        OrderShipped shipped = new OrderShipped("o-1");

        // act
        Optional<RoutingTarget> local = EventExternalizationConfiguration
                .byApplicationLocalName(List.of(APP + ".billing", APP)).apply(shipped);
        Optional<RoutingTarget> outside = EventExternalizationConfiguration.byApplicationLocalName(List.of(APP))
                .apply(new Outside("x"));
        Optional<ParsedRoutingTarget> declared = EventExternalizationConfiguration.byExternalizedAnnotations()
                .apply(shipped);
        RoutingTarget typeName = EventExternalizationConfiguration.byFullyQualifiedTypeName().apply(shipped);
        boolean externalized = EventExternalizationConfiguration.annotatedAsExternalized().test(shipped);

        // assert
        assertThat(local).contains(RoutingTarget.forTarget("order.OrderShipped"));
        assertThat(outside).isEmpty();
        assertThat(declared).contains(ParsedRoutingTarget.parse("shipments::#{#this.orderId()}"));
        assertThat(typeName).isEqualTo(RoutingTarget.forTarget(OrderShipped.class.getName()));
        assertThat(externalized).isTrue();
    }

    @Test
    void needsPackagesToRouteByApplicationLocalName() {
        // act & assert
        assertThatThrownBy(() -> EventExternalizationConfiguration.byApplicationLocalName(null))
                .isInstanceOf(NullPointerException.class).hasMessage("packages");
    }
}
