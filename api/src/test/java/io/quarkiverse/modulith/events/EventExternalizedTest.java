package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.fixtures.app.order.OrderPlaced;

class EventExternalizedTest {

    private static final RoutingTarget ORDERS = RoutingTarget.forTarget("orders");

    @Test
    void exposesWhatWasSent() {
        // arrange
        OrderPlaced event = new OrderPlaced("o-1");

        // act
        EventExternalized<OrderPlaced, String> sent = new EventExternalized<>(event, "{}", ORDERS, "offset 7");

        // assert
        assertThat(sent.getEvent()).isSameAs(event);
        assertThat(sent.getMapped()).isEqualTo("{}");
        assertThat(sent.getTarget()).isEqualTo(ORDERS);
        assertThat(sent.getBrokerResult()).isEqualTo("offset 7");
        assertThat(sent.getEventType()).isEqualTo(OrderPlaced.class);
        assertThat(sent.getType()).isEqualTo(OrderPlaced.class);
        assertThat(sent).hasToString("EventExternalized[" + OrderPlaced.class.getName() + " to orders]");
    }

    @Test
    void comparesEventMappedPayloadAndResult() {
        // arrange
        EventExternalized<OrderPlaced, String> sent = new EventExternalized<>(new OrderPlaced("o-1"), "{}", ORDERS,
                "r");

        // act
        EventExternalized<OrderPlaced, String> elsewhere = new EventExternalized<>(new OrderPlaced("o-1"), "{}",
                RoutingTarget.forTarget("x"), "r");

        // assert
        assertThat(sent)
                .isEqualTo(elsewhere)
                .hasSameHashCodeAs(new EventExternalized<>(new OrderPlaced("o-1"), "{}", ORDERS, "r"))
                .isNotEqualTo(new EventExternalized<>(new OrderPlaced("o-2"), "{}", ORDERS, "r"))
                .isNotEqualTo(new EventExternalized<>(new OrderPlaced("o-1"), "[]", ORDERS, "r"))
                .isNotEqualTo(new EventExternalized<>(new OrderPlaced("o-1"), "{}", ORDERS, null))
                .isNotEqualTo("o-1");
    }

    @ParameterizedTest
    @MethodSource("missingParts")
    void requiresEventPayloadAndTarget(Object event, Object mapped, RoutingTarget target, String message) {
        // act & assert
        assertThatThrownBy(() -> new EventExternalized<>(event, mapped, target, null))
                .isInstanceOf(NullPointerException.class).hasMessage(message);
    }

    static List<Object[]> missingParts() {
        return List.of(new Object[] { null, "{}", ORDERS, "event" }, new Object[] { "e", null, ORDERS, "mapped" },
                new Object[] { "e", "{}", null, "target" });
    }
}
