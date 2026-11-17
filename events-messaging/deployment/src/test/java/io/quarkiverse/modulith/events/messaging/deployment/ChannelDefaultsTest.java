package io.quarkiverse.modulith.events.messaging.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

class ChannelDefaultsTest {

    private static final Map.Entry<String, String> ACKS = entry("mp.messaging.outgoing.orders.acks", "all");
    private static final Map.Entry<String, String> IDEMPOTENCE = entry(
            "mp.messaging.outgoing.orders.enable.idempotence", "true");
    private static final Map.Entry<String, String> SERIALIZER = entry("mp.messaging.outgoing.orders.value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
    private static final Map.Entry<String, String> CONFIRMS = entry("mp.messaging.outgoing.orders.publish-confirms",
            "true");

    static Stream<Arguments> defaults() {
        return Stream.of(
                Arguments.of(true, true, true, Optional.empty(), Map.ofEntries(ACKS, IDEMPOTENCE, SERIALIZER, CONFIRMS)),
                Arguments.of(true, true, false, Optional.empty(), Map.ofEntries(ACKS, IDEMPOTENCE, CONFIRMS)),
                Arguments.of(true, true, true, Optional.of("smallrye-kafka"), Map.ofEntries(ACKS, IDEMPOTENCE, SERIALIZER)),
                Arguments.of(true, true, true, Optional.of("smallrye-rabbitmq"), Map.ofEntries(CONFIRMS)),
                Arguments.of(true, true, true, Optional.of("smallrye-amqp"), Map.of()),
                Arguments.of(false, true, true, Optional.of("smallrye-kafka"), Map.of()),
                Arguments.of(true, false, true, Optional.of("smallrye-rabbitmq"), Map.of()),
                Arguments.of(false, false, true, Optional.empty(), Map.of()));
    }

    @ParameterizedTest
    @MethodSource
    void defaults(boolean kafka, boolean rabbitmq, boolean json, Optional<String> connector,
            Map<String, String> expected) {
        // arrange
        MessagingConnectorsBuildItem connectors = new MessagingConnectorsBuildItem(kafka, rabbitmq, false);

        // act
        Map<String, String> defaults = EventsMessagingProcessor.channelDefaults(Set.of("orders"), connectors, json,
                channel -> connector).stream()
                .collect(Collectors.toMap(RunTimeConfigurationDefaultBuildItem::getKey,
                        RunTimeConfigurationDefaultBuildItem::getValue));

        // assert
        assertThat(defaults).isEqualTo(expected);
    }
}
