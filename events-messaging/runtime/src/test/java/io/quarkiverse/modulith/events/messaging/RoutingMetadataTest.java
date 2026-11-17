package io.quarkiverse.modulith.events.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.RoutingTarget;

class RoutingMetadataTest {

    @Test
    void addsTheRoutingTargetHeadersOfQuarkusAndSpringModulith() {
        // act
        RoutingMetadata metadata = RoutingMetadata.of(RoutingTarget.forTarget("orders").andKey("o-1"),
                Map.of("tenant", "acme"));

        // assert
        assertThat(metadata.target()).isEqualTo(RoutingTarget.forTarget("orders").andKey("o-1"));
        assertThat(metadata.headers()).isEqualTo(Map.of("tenant", "acme", "quarkusModulith_routingTarget",
                "orders::o-1", "springModulith_routingTarget", "orders::o-1"));
    }

    @Test
    void copiesTheHeaders() {
        // arrange
        Map<String, Object> headers = new HashMap<>(Map.of("tenant", "acme"));
        RoutingMetadata metadata = new RoutingMetadata(RoutingTarget.forTarget("orders"), headers);

        // act
        headers.put("other", "value");

        // assert
        assertThat(metadata.headers()).isEqualTo(Map.of("tenant", "acme"));
    }

    @Test
    void dropsHeadersWithoutValueAndKeepsTheOrder() {
        // arrange
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("tenant", null);
        headers.put("region", "eu");

        // act
        RoutingMetadata metadata = RoutingMetadata.of(RoutingTarget.forTarget("orders"), headers);

        // assert
        assertThat(metadata.headers()).containsExactly(entry("region", "eu"),
                entry("quarkusModulith_routingTarget", "orders"), entry("springModulith_routingTarget", "orders"));
    }

    @Test
    void cannotBeModified() {
        // arrange
        RoutingMetadata metadata = new RoutingMetadata(RoutingTarget.forTarget("orders"), Map.of("tenant", "acme"));

        // act & assert
        assertThatThrownBy(() -> metadata.headers().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresATarget() {
        // act & assert
        assertThatThrownBy(() -> new RoutingMetadata(null, Map.of())).isInstanceOf(NullPointerException.class)
                .hasMessage("target");
    }
}
