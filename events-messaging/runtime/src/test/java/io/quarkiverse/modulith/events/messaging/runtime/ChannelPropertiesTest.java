package io.quarkiverse.modulith.events.messaging.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChannelPropertiesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "orders|topic|mp.messaging.outgoing.orders.topic",
            "order.OrderPlaced|publish-confirms|mp.messaging.outgoing.\"order.OrderPlaced\".publish-confirms",
            "eu-orders|value.serializer|mp.messaging.outgoing.eu-orders.value.serializer" })
    void quotesChannelNamesWithDots(String channel, String attribute, String expected) {
        // act
        String property = ChannelProperties.outgoing(channel, attribute);

        // assert
        assertThat(property).isEqualTo(expected);
    }
}
