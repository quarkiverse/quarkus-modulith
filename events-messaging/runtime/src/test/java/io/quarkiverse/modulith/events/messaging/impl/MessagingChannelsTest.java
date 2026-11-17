package io.quarkiverse.modulith.events.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MessagingChannelsTest {

    @Test
    void findsChannelsByTargetAndTheDefaultChannel() {
        // arrange
        TestChannels channels = new TestChannels("fallback");
        NoopEmitter orders = new NoopEmitter();
        NoopEmitter fallback = new NoopEmitter();

        // act
        channels.register("orders", orders);
        channels.register("fallback", fallback);

        // assert
        assertThat(channels.channel("orders")).containsSame(orders);
        assertThat(channels.channel("unknown")).isEmpty();
        assertThat(channels.defaultChannel()).containsSame(fallback);
        assertThat(channels.names()).containsExactly("fallback", "orders");
    }

    @Test
    void hasNoDefaultChannelUnlessConfigured() {
        // arrange
        TestChannels channels = new TestChannels(null);

        // act
        channels.register("orders", new NoopEmitter());

        // assert
        assertThat(channels.defaultChannel()).isEmpty();
    }

    @Test
    void keepsItsChannelsWhenCallersTryToRemoveOne() {
        // arrange
        TestChannels channels = new TestChannels(null);
        channels.register("orders", new NoopEmitter());

        // act & assert
        assertThatThrownBy(() -> channels.names().remove("orders")).isInstanceOf(UnsupportedOperationException.class);
    }
}
