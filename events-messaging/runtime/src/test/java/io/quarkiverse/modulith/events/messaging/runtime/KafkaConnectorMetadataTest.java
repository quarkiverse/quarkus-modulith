package io.quarkiverse.modulith.events.messaging.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

class KafkaConnectorMetadataTest {

    static Stream<Arguments> headerValues() {
        return Stream.of(
                Arguments.of("acme", "acme".getBytes(StandardCharsets.UTF_8)),
                Arguments.of(42, "42".getBytes(StandardCharsets.UTF_8)),
                Arguments.of(new byte[] { 1, 2 }, new byte[] { 1, 2 }),
                Arguments.of("é", new byte[] { (byte) 0xC3, (byte) 0xA9 }));
    }

    @ParameterizedTest
    @MethodSource("headerValues")
    void writesHeaderValuesAsBytes(Object value, byte[] expected) {
        // act
        var headers = KafkaConnectorMetadata.headers(Map.of("header", value));

        // assert
        assertThat(headers).singleElement().satisfies(header -> {
            assertThat(header.key()).isEqualTo("header");
            assertThat(header.value()).isEqualTo(expected);
        });
    }

    @Test
    void keepsNullHeaderValues() {
        // arrange
        Map<String, Object> headers = new HashMap<>();
        headers.put("header", null);

        // act
        var recordHeaders = KafkaConnectorMetadata.headers(headers);

        // assert
        assertThat(recordHeaders).extracting(RecordHeader::value).containsExactly((byte[]) null);
    }

    @Test
    void usesTheKeyAsRecordKeyAndLeavesTheTopicToTheChannel() {
        // act
        OutgoingKafkaRecordMetadata<?> metadata = (OutgoingKafkaRecordMetadata<?>) new KafkaConnectorMetadata()
                .create(new OutgoingRouting(RoutingTarget.forTarget("orders").andKey("o-1"), Map.of("tenant", "acme"),
                        false, Optional.of("application/json")));

        // assert
        assertThat(metadata.getKey()).isEqualTo("o-1");
        assertThat(metadata.getTopic()).isNull();
        assertThat(metadata.getHeaders().lastHeader("tenant").value()).isEqualTo("acme".getBytes(StandardCharsets.UTF_8));
        assertThat(metadata.getHeaders().toArray()).hasSize(1);
    }

    @Test
    void setsTheTopicOfDynamicTargetsWithoutKey() {
        // act
        OutgoingKafkaRecordMetadata<?> metadata = (OutgoingKafkaRecordMetadata<?>) new KafkaConnectorMetadata()
                .create(new OutgoingRouting(RoutingTarget.forTarget("eu-orders"), Map.of(), true, Optional.empty()));

        // assert
        assertThat(metadata.getTopic()).isEqualTo("eu-orders");
        assertThat(metadata.getKey()).isNull();
        assertThat(metadata.getHeaders().toArray()).isEmpty();
    }
}
